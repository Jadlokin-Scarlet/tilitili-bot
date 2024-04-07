package com.tilitili.bot.config;

import com.tilitili.bot.socket.NewWebSocketFactory;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class WebSocketConfig {
    private final BotRobotCacheManager botRobotCacheManager;
    private final NewWebSocketFactory webSocketFactory;

    @Autowired
    public WebSocketConfig(BotRobotCacheManager botRobotCacheManager, NewWebSocketFactory webSocketFactory) {
        this.botRobotCacheManager = botRobotCacheManager;
        this.webSocketFactory = webSocketFactory;
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        upAllBotRobot();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void wsCheckJob() {
        upAllBotRobot();
    }

    private void upAllBotRobot() {
        List<BotRobot> robotList = botRobotCacheManager.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
//        List<BotRobot> robotList = Arrays.asList(botRobotCacheManager.getBotRobotById(3L), botRobotCacheManager.getBotRobotById(5L));
//        List<BotRobot> robotList = Collections.singletonList(botRobotCacheManager.getBotRobotById(22L));
//        List<BotRobot> robotList = Collections.emptyList();
        for (BotRobot bot : robotList) {
            if (BotRobotConstant.PUSH_TYPE_WS.equals(bot.getPushType())) {
                webSocketFactory.upBotBlocking(bot.getId());
            }
        }
    }
}
