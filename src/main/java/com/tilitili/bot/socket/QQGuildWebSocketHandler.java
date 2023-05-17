package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.SendMessageManager;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class QQGuildWebSocketHandler extends BotWebSocketHandler {
    private final ScheduledExecutorService executorService;
    private int sn = 0;

    public QQGuildWebSocketHandler(URI serverUri, BotRobot bot, BotService botService, SendMessageManager sendMessageManager) {
        super(serverUri, bot, botService, sendMessageManager);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received message={}", message);
    }
}
