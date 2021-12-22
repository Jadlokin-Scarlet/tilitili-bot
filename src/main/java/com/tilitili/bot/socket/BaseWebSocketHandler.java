package com.tilitili.bot.socket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BaseWebSocketHandler implements WebSocketHandler {

    private WebSocketConnectionManager webSocketConnectionManager;

    public String getUrl() {
        return null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        log.info("连接websocket成功，url={}", getUrl());
//        session.sendMessage(new PingMessage());
        sleepAndPing(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message);
        } else if (message instanceof PongMessage) {
            sleepAndPing(session);
        } else {
            log.error("Unexpected WebSocket message type: " + message);
        }
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        log.error("websocket网络异常", exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("连接关闭，reason={}", status.getReason());
//        sleepAndPing(session);
        webSocketConnectionManager.stop();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            webSocketConnectionManager.start();
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    public void setWebSocketConnectionManager(WebSocketConnectionManager webSocketConnectionManager) {
        this.webSocketConnectionManager = webSocketConnectionManager;
    }

    private void sleepAndPing(WebSocketSession session) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            log.info("发送ping消息");
            try {
                session.sendMessage(new PingMessage());
            } catch (IOException e) {
                log.error("发送心跳异常", e);
                this.webSocketConnectionManager.start();
            }
        }, 1, TimeUnit.MINUTES);
    }

//    @Async
//    @Scheduled(fixedRate = 60 * 60 * 1000)
//    public void heartBeat() throws Exception {
//        log.error("连接断开，重连");
//        webSocketConnectionManager.stop();
//        webSocketConnectionManager.start();
//    }
}
