package com.tilitili.bot.socket;

import com.tilitili.bot.service.GoCqhttpService;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {

    private final GoCqhttpService goCqhttpService;

    @Autowired
    public GoCqhttpWebSocketHandler(GoCqhttpManager goCqhttpManager, GoCqhttpService goCqhttpService) throws URISyntaxException {
        super(new URI(goCqhttpManager.getWebSocketUrl()));
        this.goCqhttpService = goCqhttpService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        if (message.contains("meta_event_type\":\"heartbeat")) return;
        goCqhttpService.syncHandleTextMessage(message);
    }

}