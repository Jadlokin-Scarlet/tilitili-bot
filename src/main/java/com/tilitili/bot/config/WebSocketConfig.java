package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.WebSocketFactory;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
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
    private final BotRobotCacheManager botRobotCacheManager;
    private final WebSocketFactory webSocketFactory;
    private final BotService botService;

    @Autowired
    public WebSocketConfig(BotRobotCacheManager botRobotCacheManager, WebSocketFactory webSocketFactory, BotService botService) {
        this.botRobotCacheManager = botRobotCacheManager;
        this.webSocketFactory = webSocketFactory;
        this.botService = botService;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        List<BotRobot> robotList = botRobotCacheManager.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
//        List<BotRobot> robotList = Collections.singletonList(botRobotCacheManager.getValidBotRobotById(5L));
        for (BotRobot bot : robotList) {
            webSocketFactory.upBotBlocking(bot.getId(), botService::syncHandleMessage);
        }
    }

    @Bean
    public MinecraftReceive minecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, BotService botService, BotSenderCacheManager botSenderCacheManager) {
        return new MinecraftReceive(jmsTemplate, taskMapper, environment, botService, botSenderCacheManager, botRobotCacheManager);
    }
}
