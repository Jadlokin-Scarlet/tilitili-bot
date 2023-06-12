package com.tilitili.bot.config;

import com.tilitili.bot.receive.MinecraftReceive;
import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.BaseWebSocketHandler;
import com.tilitili.bot.socket.BotWebSocketHandler;
import com.tilitili.bot.socket.KookWebSocketHandler;
import com.tilitili.bot.socket.QQGuildWebSocketHandler;
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
import com.tilitili.common.utils.TimeUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class WebSocketConfig implements ApplicationListener<ContextClosedEvent> {
    private final BotService botService;
    private final BotManager botManager;
    private final BotRobotMapper botRobotMapper;
    private final SendMessageManager sendMessageManager;
    private final Map<Long, BotWebSocketHandler> botWebSocketHandlerMap;

    @Autowired
    public WebSocketConfig(BotService botService, BotManager botManager, BotRobotMapper botRobotMapper, SendMessageManager sendMessageManager) {
        this.botRobotMapper = botRobotMapper;
        this.sendMessageManager = sendMessageManager;
        this.botService = botService;
        this.botManager = botManager;
        botWebSocketHandlerMap = new HashMap<>();
    }

    @PostConstruct
    public void webSocketConnectionManager() {
        List<BotRobot> robotList = botRobotMapper.getBotRobotByCondition(new BotRobotQuery().setStatus(0));
        for (BotRobot bot : robotList) {
            try {
                BotWebSocketHandler botWebSocketHandler = this.newWebSocketHandle(bot);
                botWebSocketHandler.connect();
                botWebSocketHandlerMap.put(bot.getId(), botWebSocketHandler);
                TimeUtil.millisecondsSleep(100);
            } catch (AssertException e) {
                log.warn("断言异常，message="+e.getMessage());
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }

    private BotWebSocketHandler newWebSocketHandle(BotRobot bot) {
        try {
            String wsUrl = botManager.getWebSocketUrl(bot);
            Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());
            switch (bot.getType()) {
                case BotRobotConstant.TYPE_MIRAI:
                case BotRobotConstant.TYPE_GOCQ: return new BotWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
                case BotRobotConstant.TYPE_KOOK: return new KookWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
                case BotRobotConstant.TYPE_QQ_GUILD: return new QQGuildWebSocketHandler(new URI(wsUrl), bot, botService, sendMessageManager);
                default: throw new AssertException("?");
            }
        } catch (URISyntaxException e) {
            log.warn("url解析异常", e);
            throw new AssertException("url解析异常");
        }
    }

    @Bean
    public MinecraftReceive minecraftReceive(JmsTemplate jmsTemplate, TaskMapper taskMapper, Environment environment, BotService botService, BotSenderMapper botSenderMapper) {
        return new MinecraftReceive(jmsTemplate, taskMapper, environment, botService, botSenderMapper, botRobotMapper);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            for (BaseWebSocketHandler botWebSocketHandler : botWebSocketHandlerMap.values()) {
                botWebSocketHandler.closeBlocking();
            }
        } catch (InterruptedException e) {
            log.error("优雅停机异常");
        }
    }

    public Map<Long, BotWebSocketHandler> getBotWebSocketHandlerMap() {
        return botWebSocketHandlerMap;
    }

    public void upBot(Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        BotWebSocketHandler botWebSocketHandler = botWebSocketHandlerMap.computeIfAbsent(bot.getId(), key->this.newWebSocketHandle(bot));
        botWebSocketHandler.botConnect();
    }

    public void downBot(Long id) {
        BotRobot bot = botRobotMapper.getBotRobotById(id);
        BotWebSocketHandler botWebSocketHandler = botWebSocketHandlerMap.computeIfAbsent(bot.getId(), key->this.newWebSocketHandle(bot));
        botWebSocketHandler.botClose();
    }
}
