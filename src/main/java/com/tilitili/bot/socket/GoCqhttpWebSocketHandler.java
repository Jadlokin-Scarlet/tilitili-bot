package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

@Slf4j
@Component
public class GoCqhttpWebSocketHandler extends BaseWebSocketHandler {
    private final BotService botService;
    private final BotManager botManager;

    @Autowired
    public GoCqhttpWebSocketHandler(GoCqhttpManager goCqhttpManager, BotService botService, BotManager botManager) throws URISyntaxException {
        super(new URI(goCqhttpManager.getWebSocketUrl()));
        this.botService = botService;
        this.botManager = botManager;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received [{}]",message);
        botService.syncHandleTextMessage(message, BotEmum.CIRNO);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        botManager.sendMessage(BotMessage.simpleListMessage(Arrays.asList(
                BotMessageChain.ofPlain("连接已断开，请检查。"),
                BotMessageChain.ofAtAll()
        )).setSenderId(3777L));
        super.onClose(code, reason, remote);
    }
}