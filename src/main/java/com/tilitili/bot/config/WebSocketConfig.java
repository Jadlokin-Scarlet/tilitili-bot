package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.rank.TaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

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

    @Bean
    public MinecraftReceive minecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, MinecraftManager minecraftManager, BotService botService, BotSenderMapper botSenderMapper) {
        return new MinecraftReceive(jmsTemplate, taskMapper, environment, minecraftManager, botService, botSenderMapper);
    }
}
