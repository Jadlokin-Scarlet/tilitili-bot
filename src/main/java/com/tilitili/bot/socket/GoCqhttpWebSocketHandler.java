package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotMessageRecordManager;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {
    private final BotMessageRecordManager botMessageRecordManager;
    private final GoCqhttpManager goCqhttpManager;
    private final BotService botService;

    @Autowired
    public GoCqhttpWebSocketHandler(BotMessageRecordManager botMessageRecordManager, GoCqhttpManager goCqhttpManager, BotService botService) throws URISyntaxException {
        super(new URI(goCqhttpManager.getWebSocketUrl()));
        this.botMessageRecordManager = botMessageRecordManager;
        this.goCqhttpManager = goCqhttpManager;
        this.botService = botService;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        if (message.contains("meta_event_type\":\"heartbeat")) return;
        boolean isGuild = message.contains("\"message_type\":\"guild\"");
        boolean isWhiteGroup = message.contains("message_type\":\"group") && message.contains("group_id\":536056405");
        boolean isWhiteFriend = message.contains("message_type\":\"private") && message.contains("user_id\":545459363");
        if (!isGuild && !isWhiteGroup && !isWhiteFriend) return;
        BotMessage botMessage = goCqhttpManager.handleGoCqhttpWsMessageToBotMessage(message);
        botMessageRecordManager.logRecord(message, botMessage);
        botService.syncHandleTextMessage(botMessage);
    }

}