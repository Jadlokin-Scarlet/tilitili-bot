package com.tilitili.bot.socket;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

@Slf4j
public class BaseWebSocketHandler extends WebSocketClient {
    public BaseWebSocketHandler(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("连接websocket成功，url={}", getURI().toString());
    }

    @Override
    public void onMessage(String message) {
        handleTextMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("连接关闭，code ={}, reason={}, remote={}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        log.error("websocket异常", ex);
    }

    protected void handleTextMessage(String message) {
    }
}
