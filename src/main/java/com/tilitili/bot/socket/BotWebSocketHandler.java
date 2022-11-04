package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Arrays;

@Slf4j
public class BotWebSocketHandler extends BaseWebSocketHandler {
    protected final BotEmum bot;
    protected final BotService botService;
    protected final BotManager botManager;

    public BotWebSocketHandler(URI serverUri, BotEmum bot, BotService botService, BotManager botManager) {
        super(serverUri);
        this.bot = bot;
        this.botService = botService;
        this.botManager = botManager;
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received bot={} message={}", bot.text, message);
        if (message.contains("post_type\":\"meta_event")
                || message.contains("post_type\":\"notice")
                || message.contains("post_type\":\"request")) {
            botService.syncHandleEvent(bot, message);
        } else {
            botService.syncHandleTextMessage(message, this.bot);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        BotMessage botMessage = BotMessage.simpleListMessage(Arrays.asList(
                BotMessageChain.ofPlain("连接已断开，请检查。")
        ));
        if (BotEmum.TYPE_GOCQ.equals(bot.getType())) {
            botMessage.setSenderId(3777L);
            botManager.sendMessage(botMessage);
        } else if (BotEmum.TYPE_MIRAI.equals(bot.getType())) {
            botMessage.setSenderId(4380L);
            botManager.sendMessage(botMessage);
        }
        super.onClose(code, reason, remote);
    }
}
