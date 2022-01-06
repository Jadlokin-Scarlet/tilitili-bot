package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotMessageRecordManager;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class MiraiWebSocketHandler extends BaseWebSocketHandler {
    private final BotMessageRecordManager botMessageRecordManager;
    private final BotService botService;
    private final MiraiManager miraiManager;

    @Autowired
    public MiraiWebSocketHandler(BotMessageRecordManager botMessageRecordManager, MiraiManager miraiManager, BotService botService) throws URISyntaxException {
        super(new URI(miraiManager.getWebSocketUrl()));
        this.botMessageRecordManager = botMessageRecordManager;
        this.miraiManager = miraiManager;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        BotMessage botMessage = miraiManager.handleMiraiWsMessageToBotMessage(message);
        botMessageRecordManager.logRecord(message, botMessage);
        botService.syncHandleTextMessage(botMessage);
    }

}