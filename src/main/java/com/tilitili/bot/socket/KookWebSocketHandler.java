package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.kook.KookWsData;
import com.tilitili.common.manager.BotManager;
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

    public KookWebSocketHandler(URI serverUri, BotEmum bot, BotService botService, BotManager botManager) {
        super(serverUri, bot, botService, botManager);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void handleTextMessage(String message) {
        log.debug("Message Received message={}", message);
        KookWsData<?> kookWsData = Gsons.fromJson(message, KookWsData.class);
        switch (kookWsData.getS()) {
            case 0: {
                this.sn = kookWsData.getSn() == null ? 0 : kookWsData.getSn();
                if (message.contains("type\":\"255")) {
                    botService.syncHandleEvent(bot, message);
                } else {
                    botService.syncHandleTextMessage(message, bot);
                }
                break;
            }
            case 1: case 3: executorService.schedule(() -> this.send("{\"s\": 2,\"sn\": "+sn+"}"), 30, TimeUnit.SECONDS); break;
            default: log.warn("记录s="+kookWsData.getS());
        }
    }
}
