package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.*;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.manager.SendMessageManager;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig implements ApplicationListener<ContextClosedEvent> {
    private final List<BaseWebSocketHandler> botWebSocketHandlerList;
    private final BotService botService;
    private final BotManager botManager;
    private final MiraiManager miraiManager;
    private final SendMessageManager sendMessageManager;

    @Autowired
    public WebSocketConfig(BotService botService, BotManager botManager, MiraiManager miraiManager, SendMessageManager sendMessageManager) {
        this.sendMessageManager = sendMessageManager;
        botWebSocketHandlerList = new ArrayList<>();
        this.miraiManager = miraiManager;
        this.botService = botService;
        this.botManager = botManager;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        for (BotEnum bot : BotEnum.values()) {
            try {
                BotWebSocketHandler botWebSocketHandler = newWebSocketHandle(bot);
                botWebSocketHandler.connect();
                botWebSocketHandlerList.add(botWebSocketHandler);

                if (BotEnum.TYPE_MIRAI.equals(bot.getType())) {
                    String eventWsUrl = miraiManager.getEventWebSocketUrl(bot);
                    BotEventWebSocketHandler botEventWebSocketHandler = new BotEventWebSocketHandler(new URI(eventWsUrl), bot, botService);
                    botEventWebSocketHandler.connect();
                    botWebSocketHandlerList.add(botEventWebSocketHandler);
                }
            } catch (AssertException e) {
                log.error("断言异常", e);
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }

    private BotWebSocketHandler newWebSocketHandle(BotEnum bot) throws URISyntaxException {
        String wsUrl = botManager.getWebSocketUrl(bot);
        Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.text);
        switch (bot.getType()) {
            case BotEnum.TYPE_MIRAI: return new BotWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            case BotEnum.TYPE_GOCQ: return new BotWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            case BotEnum.TYPE_KOOK: return new KookWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            default: throw new AssertException("?");
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
