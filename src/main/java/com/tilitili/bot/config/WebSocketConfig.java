package com.tilitili.bot.config;

import com.tilitili.bot.socket.WebSocketFactory;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
//@Configuration
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

    @Scheduled(cron = "0 0/1 * * * ?")
    public void wsCheckJob() {
        upAllBotRobot();
    }

    private void upAllBotRobot() {
        List<BotRobot> robotList = botRobotCacheManager.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
//        List<BotRobot> robotList = Collections.singletonList(botRobotCacheManager.getBotRobotById(21L));
//        List<BotRobot> robotList = Collections.emptyList();
        for (BotRobot bot : robotList) {
            if (BotRobotConstant.PUSH_TYPE_WS.equals(bot.getPushType())) {
                webSocketFactory.upBotBlocking(bot.getId());
            }
        }
    }
}
