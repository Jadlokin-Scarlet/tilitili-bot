package com.tilitili.bot.socket;

import com.tilitili.bot.service.MiraiService;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class MiraiWebSocketHandler extends BaseWebSocketHandler {

    private final MiraiService miraiService;

    @Autowired
    public MiraiWebSocketHandler(MiraiManager miraiManager, MiraiService miraiService) throws URISyntaxException {
        super(new URI(miraiManager.getWebSocketUrl()));
        this.miraiService = miraiService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        miraiService.syncHandleTextMessage(message);
    }

}