package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.bot.socket.BotEventWebSocketHandler;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.rank.TaskMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig implements ApplicationListener<ContextClosedEvent> {
    private final List<BaseWebSocketHandler> botWebSocketHandlerList;
    private final BotService botService;
    private final BotManager botManager;
    private final MiraiManager miraiManager;

    @Autowired
    public WebSocketConfig(BotService botService, BotManager botManager, MiraiManager miraiManager) {
        botWebSocketHandlerList = new ArrayList<>();
        this.miraiManager = miraiManager;
        this.botService = botService;
        this.botManager = botManager;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        for (BotEmum bot : BotEmum.values()) {
            try {
                String wsUrl = botManager.getWebSocketUrl(bot);
                Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.qq);
                BotWebSocketHandler botWebSocketHandler = new BotWebSocketHandler(new URI(wsUrl), bot, botService, botManager);
                botWebSocketHandler.connect();
                botWebSocketHandlerList.add(botWebSocketHandler);

                if (BotEmum.TYPE_MIRAI.equals(bot.getType())) {
                    String eventWsUrl = miraiManager.getEventWebSocketUrl(bot);
                    BotEventWebSocketHandler botEventWebSocketHandler = new BotEventWebSocketHandler(new URI(eventWsUrl), bot, botService);
                    botEventWebSocketHandler.connect();
                    botWebSocketHandlerList.add(botEventWebSocketHandler);
                }
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
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            for (BaseWebSocketHandler botWebSocketHandler : botWebSocketHandlerList) {
                botWebSocketHandler.closeBlocking();
            }
        } catch (InterruptedException e) {
            log.error("优雅停机异常");
        }
    }
}
