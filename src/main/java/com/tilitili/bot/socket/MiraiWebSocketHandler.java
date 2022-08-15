package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotMessageRecordManager;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Slf4j
@Component
public class MiraiWebSocketHandler extends BaseWebSocketHandler {
    private final BotMessageRecordManager botMessageRecordManager;
    private final BotService botService;
    private final MiraiManager miraiManager;
    private final BotSenderManager botSenderManager;
    private final BotManager botManager;

    @Autowired
    public MiraiWebSocketHandler(BotMessageRecordManager botMessageRecordManager, MiraiManager miraiManager, BotService botService, BotSenderManager botSenderManager, BotManager botManager) throws URISyntaxException {
        super(new URI(miraiManager.getWebSocketUrl()));
        this.botMessageRecordManager = botMessageRecordManager;
        this.miraiManager = miraiManager;
        this.botService = botService;
        this.botSenderManager = botSenderManager;
        this.botManager = botManager;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        BotMessage botMessage = miraiManager.handleMiraiWsMessageToBotMessage(message);
        if (botMessage == null) return;
        BotSender botSender = botSenderManager.getSenderByBotMessage(botMessage);
        if (!Objects.equals(botSender.getBot(), BotEmum.DAI_YOU_SEI.value)) return;
        botMessageRecordManager.logRecord(message, botMessage);
        botService.syncHandleTextMessage(botMessage, botSender);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        botManager.sendMessage(BotMessage.simpleTextMessage("连接已断开，请检查。").setSenderId(4380L));
        super.onClose(code, reason, remote);
    }

}