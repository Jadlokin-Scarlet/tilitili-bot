package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.kook.ws.KookData;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KookWebSocketHandler extends BotWebSocketHandler {
    private int sn = 0;

    public KookWebSocketHandler(URI serverUri, BotRobot bot, BotService botService, SendMessageManager sendMessageManager) {
        super(serverUri, bot, botService, sendMessageManager);
    }

    @Override
    public void handleTextMessage(String message) {
        KookData<?> kookData = Gsons.fromJson(message, KookData.class);
        switch (kookData.getS()) {
            case 0: {
                log.info("Message Received message={}", message);
                this.sn = kookData.getSn() == null ? 0 : kookData.getSn();
                botService.syncHandleMessage(bot, message);
                break;
            }
            case 1: case 3: executorService.schedule(() -> this.send("{\"s\": 2,\"sn\": "+sn+"}"), 20, TimeUnit.SECONDS); break;
            default: log.warn("记录s="+ kookData.getS());
        }
    }
}
