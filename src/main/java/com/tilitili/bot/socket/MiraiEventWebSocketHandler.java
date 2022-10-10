package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class MiraiEventWebSocketHandler extends BaseWebSocketHandler {
    private final BotService botService;

    @Autowired
    public MiraiEventWebSocketHandler(MiraiManager miraiManager, BotService botService) throws URISyntaxException {
        super(new URI(miraiManager.getEventWebSocketUrl(BotEmum.DAI_YOU_SEI)));
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        botService.syncHandleEvent(BotEmum.DAI_YOU_SEI, message);
    }

}