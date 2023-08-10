package com.tilitili.bot.socket;

import com.tilitili.common.entity.BotRobot;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.function.BiConsumer;

@Slf4j
public class BotWebSocketHandler extends BaseWebSocketHandler {
    protected final Long botId;
    protected final WebSocketFactory webSocketFactory;
    protected final BiConsumer<BotRobot, String> callback;

    public BotWebSocketHandler(URI serverUri, BotRobot bot, WebSocketFactory webSocketFactory, BiConsumer<BotRobot, String> callback) {
        super(serverUri);
        this.botId = bot.getId();
        this.webSocketFactory = webSocketFactory;
        this.callback = callback;
    }

    @Override
    protected void handleTextMessage(String message) {
        BotRobot bot = webSocketFactory.getValidBotRobotById(botId);
        this.handleBotMessage(bot, message);
    }

    protected void handleBotMessage(BotRobot bot, String message) {
        if (!message.contains("heartbeat")) {
            log.info("Message Received bot={} message={}", bot.getName(), message);
        }
        callback.accept(bot, message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        super.onClose(code, reason, remote);
    }
}
