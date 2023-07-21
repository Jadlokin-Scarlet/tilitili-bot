package com.tilitili.bot.socket;

import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BaseWebSocketHandler extends WebSocketClient {
    protected Integer status = -1;
    protected final ScheduledExecutorService executorService;

    public BaseWebSocketHandler(URI serverUri) {
        super(serverUri);
        this.executorService = Executors.newScheduledThreadPool(10);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        status = 0;
        log.info("连接websocket成功，url={}", getURI().toString());
    }

    @Override
    public void onMessage(String message) {
        try {
            handleTextMessage(StringUtils.removeCfCode(message));
        } catch (AssertException e) {
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
            status = -1;
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

    public Integer getStatus() {
        return status;
    }

    public void botConnect() {
        if (super.getSocket() != null && super.getSocket().isClosed()) {
            super.reconnect();
        } else {
            super.connect();
        }
    }

    public void botClose() {
        super.close();
    }
}
