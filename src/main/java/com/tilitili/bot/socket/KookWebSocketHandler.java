package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.view.bot.kook.ws.KookData;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KookWebSocketHandler extends BotWebSocketHandler {
    private final ScheduledExecutorService executorService;
    private int sn = 0;

    public KookWebSocketHandler(URI serverUri, BotEnum bot, BotService botService, SendMessageManager sendMessageManager) {
        super(serverUri, bot, botService, sendMessageManager);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received message={}", message);
        KookData<?> kookData = Gsons.fromJson(message, KookData.class);
        switch (kookData.getS()) {
            case 0: {
                this.sn = kookData.getSn() == null ? 0 : kookData.getSn();
                botService.testHandleMessage(bot, message);
                break;
            }
            case 1: case 3: executorService.schedule(() -> this.send("{\"s\": 2,\"sn\": "+sn+"}"), 30, TimeUnit.SECONDS); break;
            default: log.warn("记录s="+ kookData.getS());
        }
    }
}
