package com.tilitili.bot.socket;

import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.qqGuild.QQGuildWsResponse;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QQGuildWebSocketHandler extends BotWebSocketHandler {
    private int s = 0;
    private String sessionId;

    public QQGuildWebSocketHandler(URI serverUri, BotRobot bot, WebSocketFactory webSocketFactory) {
        super(serverUri, bot, webSocketFactory);
    }

    @Override
    protected void handleBotMessage(BotRobot bot, String message) {
        try {
            if ("{\"op\":9,\"d\":false}".equals(message)) {
                log.warn("{\"op\":9,\"d\":false}");
                return;
            }
            if (message.contains("\"t\":\"RESUMED\"")) {
                log.warn(message);
                s = Integer.parseInt(StringUtils.patten1("\"s\":(\\d+),", message));
                return;
            }
            QQGuildWsResponse response = Gsons.fromJson(message, QQGuildWsResponse.class);
            if (response.getOp() != 11) {
                log.info("Message Received bot={} message={}", bot.getName(), message);
            }
            switch (response.getOp()) {
                case 10: {
                    if (sessionId == null) {
                        this.send("{\"op\":2,\"d\":{\"token\":\"Bot "+bot.getVerifyKey()+"\",\"intents\":"+bot.getIntents()+",\"shard\":[0,1]}}");
                    } else {
                        this.send("{\"op\":6,\"d\":{\"token\":\"Bot "+bot.getVerifyKey()+"\",\"session_id\":\""+sessionId+"\",\"seq\":"+s+"}}");
                    }
                    executorService.schedule(() -> this.send("{\"op\": 1,\"d\": " + s + "}"), 50, TimeUnit.SECONDS);
                    break;
                }
                case 11: executorService.schedule(() -> this.send("{\"op\": 1,\"d\": "+s+"}"), 50, TimeUnit.SECONDS);break;
                case 0: {
                    this.s = response.getS();
                    if (response.getD().getSessionId() != null) {
                        this.sessionId = response.getD().getSessionId();
                    }
                    webSocketFactory.syncHandleMessage(bot, message);
                    break;
                }
                case 7: {
                    log.info("尝试重连");
                    this.send("{\"op\":6,\"d\":{\"token\":\"Bot "+bot.getVerifyKey()+"\",\"session_id\":\""+sessionId+"\",\"seq\":"+s+"}}");
                    executorService.schedule(() -> this.send("{\"op\": 1,\"d\": " + s + "}"), 50, TimeUnit.SECONDS);
                }
                default: log.warn("记录未知类型"+ message);
            }
        } catch (Exception e) {
            log.error("处理websocket异常, message="+message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        super.onClose(code, reason, remote);
    }

}
