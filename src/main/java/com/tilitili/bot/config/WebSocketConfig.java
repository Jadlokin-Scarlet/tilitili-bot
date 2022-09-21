package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.rank.TaskMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig implements DisposableBean {
    private final List<BaseWebSocketHandler> webSocketHandlerList;
    private final List<BotWebSocketHandler> botWebSocketHandlerList;
    private final BotService botService;
    private final BotManager botManager;

    @Autowired
    public WebSocketConfig(List<BaseWebSocketHandler> webSocketHandlerList, BotService botService, BotManager botManager) {
        this.webSocketHandlerList = webSocketHandlerList;
        botWebSocketHandlerList = new ArrayList<>();
        this.botService = botService;
        this.botManager = botManager;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        for (BotEmum bot : BotEmum.values()) {
            try {
                String wsUrl = botManager.getWebSocketUrl(bot);
                Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.value);
                BotWebSocketHandler botWebSocketHandler = new BotWebSocketHandler(new URI(wsUrl), bot, botService, botManager);
                botWebSocketHandler.connect();
                botWebSocketHandlerList.add(botWebSocketHandler);
            } catch (Exception e) {
                log.error("异常", e);
            }
        }


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

    @Override
    public void destroy() throws Exception {
        for (BotWebSocketHandler botWebSocketHandler : botWebSocketHandlerList) {
            botWebSocketHandler.close();
        }
        for (BaseWebSocketHandler baseWebSocketHandler : webSocketHandlerList) {
            baseWebSocketHandler.close();
        }
    }
}
