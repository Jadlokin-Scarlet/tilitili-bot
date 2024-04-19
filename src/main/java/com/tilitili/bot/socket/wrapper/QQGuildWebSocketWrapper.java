package com.tilitili.bot.socket.wrapper;

import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.handle.BotWebSocketHandler;
import com.tilitili.bot.socket.handle.QQGuildWebSocketHandler;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.HttpRequestDTO;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class QQGuildWebSocketWrapper implements BotWebSocketWrapperImp {
	private final Long botId;
	private final BotManager botManager;
	private final BotService botService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final AtomicBoolean blocking = new AtomicBoolean(false);

	private BotWebSocketHandler handler;

	public QQGuildWebSocketWrapper(Long botId, BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) {
		this.botId = botId;
		this.botManager = botManager;
		this.botService = botService;
		this.botRobotCacheManager = botRobotCacheManager;
	}

	@Override
	public void downBotBlocking() {
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
		BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
		String token = botManager.getAccessToken(bot);
		if (token == null) {
			return 0;
		}
		if (handler == null) {
			return -1;
		}
		int status = handler.getStatus();
		return status == 0? 0: -1;
	}

	@Override
	public void upBotBlocking() {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(botId);
		Asserts.notNull(bot, "权限不足");
		HttpRequestDTO wsRequest = botManager.getWebSocketUrl(bot);
		Asserts.notNull(wsRequest.getUrl(), "%s获取ws地址异常", bot.getName());

		this.upBotBlocking(wsRequest, bot);
	}

	private void upBotBlocking(HttpRequestDTO wsRequest, BotRobot bot) {
		try {
			String token = botManager.getAccessToken(bot);
			Asserts.notNull(token, "获取token失败 botId="+bot.getId());
//			if (token == null) {
//				return;
//			}
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
			QQGuildWebSocketHandler newHandler = new QQGuildWebSocketHandler(wsRequest, bot, botManager, botService, botRobotCacheManager);
			newHandler.connectBlocking();
			handler = newHandler;
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
