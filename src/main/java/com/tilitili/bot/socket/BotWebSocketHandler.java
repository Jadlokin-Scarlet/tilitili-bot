package com.tilitili.bot.socket;

import com.tilitili.common.entity.BotRobot;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class BotWebSocketHandler extends BaseWebSocketHandler {
    protected final Long botId;
    protected final WebSocketFactory webSocketFactory;

    public BotWebSocketHandler(URI serverUri, BotRobot bot, WebSocketFactory webSocketFactory) {
        super(serverUri);
        this.botId = bot.getId();
        this.webSocketFactory = webSocketFactory;
    }

    @Override
    protected void handleTextMessage(String message) {
        BotRobot bot = webSocketFactory.getValidBotRobotById(botId);
        if (!message.contains("heartbeat")) {
            log.info("Message Received bot={} message={}", bot.getName(), message);
        }
        this.handleBotMessage(bot, message);
    }

    protected void handleBotMessage(BotRobot bot, String message) {
        webSocketFactory.syncHandleMessage(bot, message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        super.onClose(code, reason, remote);
    }
}
