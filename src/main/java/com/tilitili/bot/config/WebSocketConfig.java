package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.bot.socket.KookWebSocketHandler;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
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
    private final BotService botService;
    private final BotManager botManager;
    private final BotRobotMapper botRobotMapper;
    private final SendMessageManager sendMessageManager;
    private final List<BaseWebSocketHandler> botWebSocketHandlerList;

    @Autowired
    public WebSocketConfig(BotService botService, BotManager botManager, BotRobotMapper botRobotMapper, SendMessageManager sendMessageManager) {
        this.botRobotMapper = botRobotMapper;
        this.sendMessageManager = sendMessageManager;
        botWebSocketHandlerList = new ArrayList<>();
        this.botService = botService;
        this.botManager = botManager;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        List<BotRobot> robotList = botRobotMapper.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
        for (BotRobot bot : robotList) {
            try {
                BotWebSocketHandler botWebSocketHandler = newWebSocketHandle(bot);
                botWebSocketHandler.connect();
                botWebSocketHandlerList.add(botWebSocketHandler);
            } catch (AssertException e) {
                log.warn("断言异常", e);
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }

    private BotWebSocketHandler newWebSocketHandle(BotRobot bot) throws URISyntaxException {
        String wsUrl = botManager.getWebSocketUrl(bot);
        Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());
        switch (bot.getType()) {
            case BotRobotConstant.TYPE_MIRAI: return new BotWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            case BotRobotConstant.TYPE_GOCQ: return new BotWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            case BotRobotConstant.TYPE_KOOK: return new KookWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
            default: throw new AssertException("?");
        }
    }

    @Bean
    public MinecraftReceive minecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, BotService botService, BotSenderMapper botSenderMapper) {
        return new MinecraftReceive(jmsTemplate, taskMapper, environment, botService, botSenderMapper, botRobotMapper);
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
