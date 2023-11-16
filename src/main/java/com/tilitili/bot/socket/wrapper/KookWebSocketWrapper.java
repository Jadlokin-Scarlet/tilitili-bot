package com.tilitili.bot.socket.wrapper;

import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.handle.BotWebSocketHandler;
import com.tilitili.bot.socket.handle.KookWebSocketHandler;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class KookWebSocketWrapper implements BotWebSocketWrapperImp {
	private final Long botId;
	private final BotManager botManager;
	private final BotService botService;
	private final BotRobotCacheManager botRobotCacheManager;
	private BotWebSocketHandler handler;
	private final AtomicBoolean blocking = new AtomicBoolean(false);


	public KookWebSocketWrapper(Long botId, BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) {
		this.botId = botId;
		this.botManager = botManager;
		this.botService = botService;
		this.botRobotCacheManager = botRobotCacheManager;
	}

	@Override
	public void downBotBlocking() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
		Asserts.notNull(bot, "权限不足");
		try {
			Asserts.isTrue(blocking.compareAndSet(false, true), "链接超时，请重试");
			if (handler != null) {
				handler.closeBlocking();
				handler = null;
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
		if (handler == null) {
			return -1;
		}
		return handler.getStatus() == 0? 0: -1;
	}

	@Override
	public void upBotBlocking() {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(botId);
		Asserts.notNull(bot, "权限不足");
		String wsUrl = botManager.getWebSocketUrl(bot);
		Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());
		try {
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
				handler = null;
			}
			handler = new KookWebSocketHandler(new URI(wsUrl), bot, botService, botRobotCacheManager);
			handler.connectBlocking();
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
