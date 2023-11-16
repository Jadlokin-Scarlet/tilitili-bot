package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.bot.socket.wrapper.BotWebSocketWrapper;
import com.tilitili.bot.socket.wrapper.BotWebSocketWrapperImp;
import com.tilitili.bot.socket.wrapper.KookWebSocketWrapper;
import com.tilitili.bot.socket.wrapper.QQGuildWebSocketWrapper;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketFactory implements ApplicationListener<ContextClosedEvent> {
    private final ConcurrentHashMap<Long, BotWebSocketWrapperImp> wrapperMap;
//    private final Long shortUrlWebSocketKey = -1L;
    private final ConcurrentHashMap<Long, Boolean> botIdLockMap = new ConcurrentHashMap<>();
    private final BotManager botManager;
    private final BotService botService;
    private final BotRobotCacheManager botRobotCacheManager;


    public WebSocketFactory(BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) {
        this.botManager = botManager;
        this.botService = botService;
        this.botRobotCacheManager = botRobotCacheManager;
        this.wrapperMap = new ConcurrentHashMap<>();
    }

    public int getWsStatus(BotRobot bot) {
        return wrapperMap.get(bot.getId()).getStatus();
    }

    private BotWebSocketWrapperImp newWebSocketWrapper(Long botId) {
        BotRobot bot = botRobotCacheManager.getValidBotRobotById(botId);
        Asserts.notNull(bot, "权限不足");
        switch (bot.getType()) {
            case BotRobotConstant.TYPE_MIRAI:
            case BotRobotConstant.TYPE_GOCQ: return (new BotWebSocketWrapper(botId, botManager, botService, botRobotCacheManager));
            case BotRobotConstant.TYPE_KOOK: return (new KookWebSocketWrapper(botId, botManager, botService, botRobotCacheManager));
            case BotRobotConstant.TYPE_QQ_GUILD: return new QQGuildWebSocketWrapper(botId, botManager, botService, botRobotCacheManager);
            default: throw new AssertException("?");
        }
    }

    public void upBotBlocking(Long botId) {
        Asserts.notNull(botId, "参数异常");
        if (!wrapperMap.containsKey(botId)) {
            synchronized (wrapperMap) {
                if (!wrapperMap.containsKey(botId)) {
                    wrapperMap.put(botId, newWebSocketWrapper(botId));
                }
            }
        }
        wrapperMap.get(botId).upBotBlocking();
    }

    public void downBotBlocking(Long botId) {
        wrapperMap.get(botId).downBotBlocking();
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        for (BotWebSocketWrapperImp wrapper : wrapperMap.values()) {
            wrapper.downBotBlocking();
        }
    }
}
