package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.qqGuild.QQGuildWsResponse;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QQGuildWebSocketHandler extends BotWebSocketHandler {
    private int s = 0;
    private String sessionId;

    public QQGuildWebSocketHandler(URI serverUri, BotRobot bot, BotService botService, SendMessageManager sendMessageManager) {
        super(serverUri, bot, botService, sendMessageManager);
    }

    @Override
    public void handleTextMessage(String message) {
        QQGuildWsResponse response = Gsons.fromJson(message, QQGuildWsResponse.class);
        switch (response.getOp()) {
            case 10: {
                this.send("{\"op\":2,\"d\":{\"token\":\"Bot 101983521.Bw0Wi9DZLzI7rVQbZmCO1Qeo0jUyuIla\",\"intents\":513,\"shard\":[0,1]}}");
                executorService.schedule(() -> this.send("{\"op\": 1,\"d\": "+s+"}"), 50, TimeUnit.SECONDS);
                break;
            }
            case 11: executorService.schedule(() -> this.send("{\"op\": 1,\"d\": "+s+"}"), 50, TimeUnit.SECONDS);break;
            case 0: {
                log.info("Message Received message={}", message);
                this.s = response.getS();
                this.sessionId = response.getD().getSessionId();
                botService.syncHandleMessage(bot, message);
                break;
            }
            default: log.warn("记录未知类型"+ message);
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        super.onClose(code, reason, remote);
//        this.send("{\"op\":6,\"d\":{\"token\":\"Bot 101983521.Bw0Wi9DZLzI7rVQbZmCO1Qeo0jUyuIla\",\"session_id\":\""+sessionId+"\",\"seq\":"+s+"}}");
//        executorService.schedule(() -> this.send("{\"op\": 1,\"d\": "+s+"}"), 50, TimeUnit.SECONDS);
    }
}
