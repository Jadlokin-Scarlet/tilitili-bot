package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.manager.BotMessageRecordManager;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class MiraiEventWebSocketHandler extends BaseWebSocketHandler {
    private final BotMessageRecordManager botMessageRecordManager;
    private final BotService botService;
    private final MiraiManager miraiManager;

    @Autowired
    public MiraiEventWebSocketHandler(BotMessageRecordManager botMessageRecordManager, MiraiManager miraiManager, BotService botService) throws URISyntaxException {
        super(new URI(miraiManager.getEventWebSocketUrl()));
        this.botMessageRecordManager = botMessageRecordManager;
        this.miraiManager = miraiManager;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
//        BotMessage botMessage = miraiManager.handleMiraiWsMessageToBotMessage(message);
//        if (botMessage == null) return;
//        botMessageRecordManager.asyncLogRecord(message, botMessage);
//        botService.syncHandleTextMessage(botMessage);
    }

}