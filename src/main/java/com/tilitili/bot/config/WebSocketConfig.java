package com.tilitili.bot.config;

import com.tilitili.bot.socket.BaseWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig {
    final List<BaseWebSocketHandler> webSocketHandlerList;

    @Autowired
    public WebSocketConfig(List<BaseWebSocketHandler> webSocketHandlerList) {
        this.webSocketHandlerList = webSocketHandlerList;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        for (BaseWebSocketHandler webSocketHandler : webSocketHandlerList) {
            try {
                webSocketHandler.connect();
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }
}
