package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class BotEventWebSocketHandler extends BaseWebSocketHandler {
    private final BotEmum bot;
    private final BotService botService;

    public BotEventWebSocketHandler(URI serverUri, BotEmum bot, BotService botService) {
        super(serverUri);
        this.bot = bot;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received bot={} message={}", bot.value, message);
        botService.syncHandleEvent(bot, message);
    }

}
