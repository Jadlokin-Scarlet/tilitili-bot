package com.tilitili.bot.socket;

import com.tilitili.bot.service.MiraiService;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class MiraiWebSocketHandler extends BaseWebSocketHandler {

    private final MiraiService miraiService;
    private final MiraiManager miraiManager;

    @Autowired
    public MiraiWebSocketHandler(MiraiManager miraiManager, MiraiService miraiService) {
        this.miraiManager = miraiManager;
        this.miraiService = miraiService;
    }

    @Override
    public String getUrl() {
        return miraiManager.getWebSocketUrl();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Message Received [{}]",message.getPayload());
        miraiService.syncHandleTextMessage(message.getPayload());
    }

}