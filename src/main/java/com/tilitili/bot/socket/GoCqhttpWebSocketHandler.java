package com.tilitili.bot.socket;

import com.tilitili.bot.service.GoCqhttpService;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {

    private final GoCqhttpManager goCqhttpManager;
    private final GoCqhttpService goCqhttpService;

    @Autowired
    public GoCqhttpWebSocketHandler(GoCqhttpManager goCqhttpManager, GoCqhttpService goCqhttpService) {
        this.goCqhttpManager = goCqhttpManager;
        this.goCqhttpService = goCqhttpService;
    }

    @Override
    public String getUrl() {
        return goCqhttpManager.getWebSocketUrl();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Message Received [{}]",message.getPayload());
        if (message.getPayload().contains("meta_event_type\":\"heartbeat")) return;
        goCqhttpService.syncHandleTextMessage(message.getPayload());
    }

}