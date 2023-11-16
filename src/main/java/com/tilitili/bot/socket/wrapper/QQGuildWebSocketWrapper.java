package com.tilitili.bot.socket.wrapper;

import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.handle.BotWebSocketHandler;
import com.tilitili.bot.socket.handle.QQGuildWebSocketHandler;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class QQGuildWebSocketWrapper implements BotWebSocketWrapperImp {
	private final Long botId;
	private final BotManager botManager;
	private final BotService botService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final Map<String, BotWebSocketHandler> handlerMap;
	private final AtomicBoolean blocking = new AtomicBoolean(false);
	private final String TYPE_GROUP = "group";
	private final String TYPE_GUILD = "guild";


	public QQGuildWebSocketWrapper(Long botId, BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) {
		this.botId = botId;
		this.botManager = botManager;
		this.botService = botService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.handlerMap = new HashMap<>();
	}

	@Override
	public void downBotBlocking() {
		this.downBotBlocking(TYPE_GROUP);
//		this.downBotBlocking(TYPE_GUILD);
	}

	public void downBotBlocking(String type) {
		try {
			Asserts.isTrue(blocking.compareAndSet(false, true), "链接超时，请重试");
			BotWebSocketHandler handler = handlerMap.get(type);
			if (handler != null) {
				handler.closeBlocking();
				handlerMap.remove(type);
			}
		} catch (AssertException e) {
			log.warn("断言异常，message="+e.getMessage());
		} catch (Exception e) {
			log.error("异常", e);
		} finally {
			blocking.set(false);
		}
	}

	@Override
	public int getStatus() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
		int groupStatus = this.getStatus(bot, TYPE_GROUP);
		int guildStatus = 0;;//this.getStatus(bot, TYPE_GUILD);
		return groupStatus == guildStatus && guildStatus == 0? 0: -1;
	}

	private int getStatus(BotRobot bot, String type) {
		String token = botManager.getAccessToken(bot, type);
		if (token == null) {
			return 0;
		}
		BotWebSocketHandler handler = handlerMap.get(type);
		if (handler == null) {
			return -1;
		}
		return handler.getStatus();
	}

	@Override
	public void upBotBlocking() {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(botId);
		Asserts.notNull(bot, "权限不足");
		String wsUrl = botManager.getWebSocketUrl(bot);
		Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());

		this.upBotBlocking(wsUrl, bot, TYPE_GROUP);
//		this.upBotBlocking(wsUrl, bot, TYPE_GUILD);
	}

	private void upBotBlocking(String wsUrl, BotRobot bot, String type) {
		try {
			String token = botManager.getAccessToken(bot, type);
			if (token == null) {
				return;
			}
			BotWebSocketHandler handler = handlerMap.get(type);
			if (handler != null && handler.getStatus() == 0) {
				return;
			}
			Asserts.isTrue(blocking.compareAndSet(false, true), "链接超时，请重试");
			log.info("尝试连接ws botId=" + bot.getId());
			if (handler != null && handler.getStatus() == 0) {
				return;
			}
			if (handler != null) {
				handler.closeBlocking();
				handlerMap.remove(type);
			}
			QQGuildWebSocketHandler newHandler = new QQGuildWebSocketHandler(new URI(wsUrl), bot, type, botManager, botService, botRobotCacheManager);
			newHandler.connectBlocking();
			handlerMap.put(type, newHandler);
			log.info("连接ws结束 botId=" + bot.getId());
		} catch (AssertException e) {
			log.warn("断言异常，message=" + e.getMessage());
		} catch (Exception e) {
			log.error("异常", e);
		} finally {
			blocking.set(false);
		}
	}
}
