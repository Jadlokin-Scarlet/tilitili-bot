package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.MiraiManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class MiraiWebSocketHandler extends BaseWebSocketHandler {
    private final BotService botService;
    private final BotManager botManager;

    @Autowired
    public MiraiWebSocketHandler(MiraiManager miraiManager, BotService botService, BotManager botManager) throws URISyntaxException {
        super(new URI(miraiManager.getWebSocketUrl()));
        this.botService = botService;
        this.botManager = botManager;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        botService.syncHandleTextMessage(message, BotEmum.DAI_YOU_SEI);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        botManager.sendMessage(BotMessage.simpleTextMessage("连接已断开，请检查。").setSenderId(4380L));
        super.onClose(code, reason, remote);
    }

}