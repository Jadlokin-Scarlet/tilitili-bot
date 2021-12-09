package com.tilitili.bot.config;

import com.tilitili.bot.socket.BaseWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig {
    final List<BaseWebSocketHandler> webSocketHandlerList;
    @Value("${project.ignore-null-websocket-container:false}")
    private boolean ignoreNullWsContainer;

    @Autowired
    public WebSocketConfig(List<BaseWebSocketHandler> webSocketHandlerList) {
        this.webSocketHandlerList = webSocketHandlerList;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        for (BaseWebSocketHandler webSocketHandler : webSocketHandlerList) {
            try {
                StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
                WebSocketConnectionManager webSocketConnectionManager = new WebSocketConnectionManager(standardWebSocketClient, webSocketHandler, webSocketHandler.getUrl());
                webSocketConnectionManager.setAutoStartup(true);
                webSocketConnectionManager.start();
                webSocketHandler.setWebSocketConnectionManager(webSocketConnectionManager);
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        if (ignoreNullWsContainer) {
            log.error("Could not initialize Websocket Container in Testcase.");
            return null;
        }
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // ws 传输数据的时候，数据过大有时候会接收不到，所以在此处设置bufferSize
        container.setMaxTextMessageBufferSize(512000);
        container.setMaxBinaryMessageBufferSize(512000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }
}
