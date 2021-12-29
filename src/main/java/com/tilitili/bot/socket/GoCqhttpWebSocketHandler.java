package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {

    private final GoCqhttpManager goCqhttpManager;
    private final BotService botService;

    @Autowired
    public GoCqhttpWebSocketHandler(GoCqhttpManager goCqhttpManager, BotService botService) throws URISyntaxException {
        super(new URI(goCqhttpManager.getWebSocketUrl()));
        this.goCqhttpManager = goCqhttpManager;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        if (message.contains("meta_event_type\":\"heartbeat")) return;
        if (! message.contains("\"message_type\":\"guild\"")) return;
        BotMessage botMessage = goCqhttpManager.handleGoCqhttpWsMessageToBotMessage(message);
        botService.syncHandleTextMessage(botMessage);
    }

}