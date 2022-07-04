package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotMessageRecordManager;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {
    private final BotMessageRecordManager botMessageRecordManager;
    private final GoCqhttpManager goCqhttpManager;
    private final BotService botService;
    private final BotSenderManager botSenderManager;

    @Autowired
    public GoCqhttpWebSocketHandler(BotMessageRecordManager botMessageRecordManager, GoCqhttpManager goCqhttpManager, BotService botService, BotSenderManager botSenderManager) throws URISyntaxException {
        super(new URI(goCqhttpManager.getWebSocketUrl()));
        this.botMessageRecordManager = botMessageRecordManager;
        this.goCqhttpManager = goCqhttpManager;
        this.botService = botService;
        this.botSenderManager = botSenderManager;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        if (message.contains("meta_event_type\":\"heartbeat")) return;
        BotMessage botMessage = goCqhttpManager.handleGoCqhttpWsMessageToBotMessage(message);
        if (botMessage == null) return;
        BotSender botSender = botSenderManager.getSenderByBotMessage(botMessage);
        if (!Objects.equals(botSender.getBot(), BotEmum.CIRNO.value)) return;
        botMessageRecordManager.logRecord(message, botMessage);
        botService.syncHandleTextMessage(botMessage, botSender);
    }

}