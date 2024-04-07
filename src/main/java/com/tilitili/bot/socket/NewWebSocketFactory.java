package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.http.WebSocket;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NewWebSocketFactory {
    private final ConcurrentHashMap<Long, WebSocket> webSocketMap;
    private final BotManager botManager;
    private final BotService botService;
    private final BotRobotCacheManager botRobotCacheManager;


    public NewWebSocketFactory(BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) {
        this.botManager = botManager;
        this.botService = botService;
        this.webSocketMap = new ConcurrentHashMap<>();
        this.botRobotCacheManager = botRobotCacheManager;
    }

    @Async
    public void upBotBlocking(Long botId) {
        Asserts.notNull(botId, "参数异常");
        BotRobot bot = botRobotCacheManager.getBotRobotById(botId);
        Asserts.notNull(bot, "权限不足");
        if (!webSocketMap.containsKey(botId)) {
            synchronized (webSocketMap) {
                if (!webSocketMap.containsKey(botId)) {
                    log.info("初始化websocket, bot={}", bot.getName());
                    try {
                        webSocketMap.put(botId, botManager.getWebSocket(bot, botService::syncHandleMessage, this::onError));
                        log.info("初始化websocket完成, bot={}", bot.getName());
                    } catch (AssertException e) {
                        log.warn("初始化websocket失败, bot={} info={}", bot.getName(), e.getMessage());
                    } catch (Exception e) {
                        log.error("初始化websocket异常, bot=" + bot.getName(), e);
                    }
                }
            }
        }
    }

    public void downBotBlocking(Long botId) {
        webSocketMap.get(botId).abort();
        webSocketMap.remove(botId);
    }

    public Integer getWsStatus(BotRobot bot) {
        WebSocket webSocket = webSocketMap.get(bot.getId());
        if (webSocket == null) {
            return -1;
        }
        return webSocket.isInputClosed() || webSocket.isOutputClosed()? -1: 0;
    }
    
    private void onError(Long botId) {
        webSocketMap.remove(botId);
    }

    public void close() {
        for (WebSocket webSocket : webSocketMap.values()) {
            webSocket.abort();
        }
    }
}
