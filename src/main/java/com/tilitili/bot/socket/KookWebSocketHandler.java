package com.tilitili.bot.socket;

import com.google.common.reflect.TypeToken;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.kook.ws.KookData;
import com.tilitili.common.entity.view.bot.kook.ws.KookSessionId;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KookWebSocketHandler extends BotWebSocketHandler {
    private int sn = 0;
    private String sessionId;

    public KookWebSocketHandler(URI serverUri, BotRobot bot, WebSocketFactory webSocketFactory) {
        super(serverUri, bot, webSocketFactory);
        this.setReuseAddr(true);
    }

    @Override
    protected void handleBotMessage(BotRobot bot, String message) {
        log.info("Message Received bot={} message={}", bot.getName(), message);
        KookData<KookSessionId> kookData = Gsons.fromJson(message, new TypeToken<KookData<KookSessionId>>(){}.getType());
        switch (kookData.getS()) {
            case 0: {
                this.sn = kookData.getSn() == null ? 0 : kookData.getSn();
                webSocketFactory.syncHandleMessage(bot, message);
                break;
            }
            case 1: if (kookData.getD() != null && kookData.getD().getSessionId() != null) this.sessionId = kookData.getD().getSessionId();
            case 3: executorService.schedule(() -> this.send("{\"s\": 2,\"sn\": "+sn+"}"), 20, TimeUnit.SECONDS); break;
            default: log.warn("记录s="+ kookData.getS());
        }
    }

    @Override
    public void connect() {
        if (sessionId != null) {
            String token = StringUtils.patten1("&token=(\\w+?==)", this.uri.toString());
            this.uri = uri.resolve(String.format("gateway?compress=0&token=%s&&resume=1&sn=%d&session_id=%s", token, sn, sessionId));
        }
        super.connect();
    }
}
