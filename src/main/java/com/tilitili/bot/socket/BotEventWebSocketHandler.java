package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEnum;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class BotEventWebSocketHandler extends BaseWebSocketHandler {
    private final BotEnum bot;
    private final BotService botService;

    public BotEventWebSocketHandler(URI serverUri, BotEnum bot, BotService botService) {
        super(serverUri);
        this.bot = bot;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received bot={} message={}", bot.text, message);
        botService.syncHandleMessage(bot, message);
    }

}
