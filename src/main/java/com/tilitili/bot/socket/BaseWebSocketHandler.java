package com.tilitili.bot.socket;

import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BaseWebSocketHandler extends WebSocketClient {
    protected final AtomicInteger status = new AtomicInteger(-1);
    protected final ScheduledExecutorService executorService;

    public BaseWebSocketHandler(URI serverUri) {
        super(serverUri);
        this.executorService = Executors.newScheduledThreadPool(10);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        status.set(0);
        log.info("连接websocket成功，url={}", getURI().toString());
    }

    @Override
    public void onMessage(String message) {
        try {
            handleTextMessage(StringUtils.removeCfCode(message));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("连接关闭，url={} code ={}, reason={}, remote={}", this.uri.toString(), code, reason, remote);
        if (remote) {
            executorService.schedule(() -> {
                log.info("尝试重连");
                this.reconnect();
            }, 60, TimeUnit.SECONDS);
        } else {
            status.set(-1);
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("websocket异常", ex);
    }

    @Override
    public void connect() {
        super.connect();
    }

    protected void handleTextMessage(String message) {
    }
}
