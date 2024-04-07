package com.tilitili.bot.socket.handle;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.HttpRequestDTO;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

@Slf4j
public class BotWebSocketHandler extends BaseWebSocketHandler {
    protected final Long botId;
    private final BotService botService;
    private final BotRobotCacheManager botRobotCacheManager;

    public BotWebSocketHandler(HttpRequestDTO wsRequest, BotRobot bot, BotService botService, BotRobotCacheManager botRobotCacheManager) throws URISyntaxException {
        super(wsRequest);
        this.botId = bot.getId();
        this.botService = botService;
        this.botRobotCacheManager = botRobotCacheManager;
    }

    @Override
    protected void handleTextMessage(String message) {
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        this.handleBotMessage(bot, message);
    }

    protected void handleBotMessage(BotRobot bot, String message) {
        if (!message.contains("heartbeat")) {
            log.info("Message Received bot={} message={}", bot.getName(), message);
        }
        botService.syncHandleMessage(bot, message);
    }
}
