package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.WebSocketFactory;
import com.tilitili.common.constant.BotRobotConstant;
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
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig {
    private final BotRobotCacheManager botRobotCacheManager;
    private final WebSocketFactory webSocketFactory;

    @Autowired
    public WebSocketConfig(BotRobotCacheManager botRobotCacheManager, WebSocketFactory webSocketFactory) {
        this.botRobotCacheManager = botRobotCacheManager;
        this.webSocketFactory = webSocketFactory;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        upAllBotRobot();
    }

    @Bean
    public MinecraftReceive minecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, BotService botService, BotSenderCacheManager botSenderCacheManager) {
        return new MinecraftReceive(jmsTemplate, taskMapper, environment, botService, botSenderCacheManager, botRobotCacheManager);
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void wsCheckJob() {
        upAllBotRobot();
    }

    private void upAllBotRobot() {
        List<BotRobot> robotList = botRobotCacheManager.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
//        List<BotRobot> robotList = Collections.singletonList(botRobotCacheManager.getBotRobotById(9L));
//        List<BotRobot> robotList = Collections.emptyList();
        for (BotRobot bot : robotList) {
            if (BotRobotConstant.PUSH_TYPE_WS.equals(bot.getPushType())) {
                webSocketFactory.upBotBlocking(bot.getId());
            }
        }
    }
}
