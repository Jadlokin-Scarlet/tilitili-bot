package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.http.WebSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class NewWebSocketFactory {
	private final ConcurrentHashMap<Long, WebSocket> webSocketMap;
	private final ConcurrentHashMap<Long, Boolean> botIdLockMap;
	private final BotManager botManager;
	private final BotService botService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final ExecutorService executor;
	private volatile boolean close = false;
	private final RedisCache redisCache;

	public NewWebSocketFactory(BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager, RedisCache redisCache) {
		this.botManager = botManager;
		this.botService = botService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.redisCache = redisCache;
		this.webSocketMap = new ConcurrentHashMap<>();
		this.botIdLockMap = new ConcurrentHashMap<>();
		this.executor = Executors.newFixedThreadPool(botRobotCacheManager.countBotRobotByCondition(new BotRobotQuery().setStatus(0)) + 1);
	}

	public void upBotBlocking(Long botId) {
		executor.submit(() -> upBotBlocking0(botId));
	}

	private void upBotBlocking0(Long botId) {
		if (close) {
			return;
		}
		Asserts.notNull(botId, "参数异常");
		BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
		Asserts.notNull(bot, "权限不足");
		try {
			if (!webSocketMap.containsKey(botId)) {
				log.info("初始化websocket, bot={}", this.logBot(bot));
				Asserts.checkNull(botIdLockMap.putIfAbsent(botId, true), "系统繁忙。");
				if (!webSocketMap.containsKey(botId)) {
					if (redisCache.exists("ws_lock_"+botId)) {
						return;
					}
					if (!redisCache.exists("ws_cnt_"+botId)) {
						redisCache.increment("ws_cnt_"+botId, 1L, 60);
					} else {
						redisCache.increment("ws_cnt_"+botId);
					}
					if (redisCache.getValueLong("ws_cnt_"+botId) > 10) {
						redisCache.setValue("ws_lock_"+botId, "yes", 60 * 10);
					}
					webSocketMap.put(botId, botManager.getWebSocket(bot, botService::syncHandleMessage, this::onClose));
					log.info("初始化websocket完成, bot={}", this.logBot(bot));
				}
			}
		} catch (AssertException e) {
			log.warn("初始化websocket失败, bot={} info={}", this.logBot(bot), e.getMessage());
		} catch (Exception e) {
			log.error("初始化websocket异常, bot=" + this.logBot(bot), e);
		} finally {
			botIdLockMap.remove(botId);
		}
	}

	public void downBotBlocking(Long botId) {
		if (webSocketMap.containsKey(botId)) {
			webSocketMap.get(botId).sendClose(WebSocket.NORMAL_CLOSURE, "");
			webSocketMap.remove(botId);
		}
	}

	public Integer getWsStatus(BotRobot bot) {
		WebSocket webSocket = webSocketMap.get(bot.getId());
		if (webSocket == null) {
			return -1;
		}
		return webSocket.isInputClosed() || webSocket.isOutputClosed()? -1: 0;
	}

	private void onClose(Long botId) {
		log.warn("重连websocket, botId={}", botId);
		webSocketMap.remove(botId);
		if (botRobotCacheManager.getValidBotRobotById(botId) != null) {
			this.upBotBlocking(botId);
		}
	}

	public void close() {
		this.close = true;
		for (WebSocket webSocket : webSocketMap.values()) {
			webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
		}
	}

	private String logBot(BotRobot bot) {
		return bot.getId() + "." + bot.getName();
	}

	public void testUp(long botId) {
		webSocketMap.remove(botId);
		botIdLockMap.remove(botId);
		upBotBlocking(botId);
	}
}
