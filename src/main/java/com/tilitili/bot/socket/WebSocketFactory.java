package com.tilitili.bot.socket;

import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class WebSocketFactory implements ApplicationListener<ContextClosedEvent> {
    private final Map<Long, BaseWebSocketHandler> botWebSocketHandlerMap;
    private final Long shortUrlWebSocketKey = -1L;
    private final ConcurrentHashMap<Long, Boolean> botIdLockMap = new ConcurrentHashMap<>();
    private final BotManager botManager;
    private final BotRobotCacheManager botRobotCacheManager;
    private final RedisCache redisCache;


    public WebSocketFactory(BotManager botManager, BotRobotCacheManager botRobotCacheManager, RedisCache redisCache) {
        this.botManager = botManager;
        this.botRobotCacheManager = botRobotCacheManager;
        this.redisCache = redisCache;
        this.botWebSocketHandlerMap = new HashMap<>();
    }

    public BotWebSocketHandler getWebSocketOrNull(BotRobot bot) {
        return (BotWebSocketHandler) botWebSocketHandlerMap.get(bot.getId());
    }

    public ShortUrlWebSocketHandler getShortUrlWebSocketHandler() {
        BaseWebSocketHandler webSocketHandler = botWebSocketHandlerMap.get(shortUrlWebSocketKey);
        if (webSocketHandler == null || webSocketHandler.getStatus() != 0) {
            this.upBotBlocking(shortUrlWebSocketKey, null);
        }
        return (ShortUrlWebSocketHandler) botWebSocketHandlerMap.get(shortUrlWebSocketKey);
    }

    private BaseWebSocketHandler newWebSocketHandle(Long key, BiConsumer<BotRobot, String> callback) {
        try {
            if (this.shortUrlWebSocketKey.equals(key)) {
                return new ShortUrlWebSocketHandler(redisCache);
            }
            BotRobot bot = botRobotCacheManager.getValidBotRobotById(key);
            Asserts.notNull(bot, "权限不足");
            String wsUrl = botManager.getWebSocketUrl(bot);
            Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());
            switch (bot.getType()) {
                case BotRobotConstant.TYPE_MIRAI:
                case BotRobotConstant.TYPE_GOCQ: return new BotWebSocketHandler(new URI(wsUrl), bot, this, callback);
                case BotRobotConstant.TYPE_KOOK: return new KookWebSocketHandler(new URI(wsUrl), bot, this, callback);
                case BotRobotConstant.TYPE_QQ_GUILD: return new QQGuildWebSocketHandler(new URI(wsUrl), bot, this, callback);
                default: throw new AssertException("?");
            }
        } catch (URISyntaxException e) {
            log.warn("url解析异常", e);
            throw new AssertException("url解析异常");
        }
    }

    public BotRobot getValidBotRobotById(Long botId) {
        BotRobot botRobot = botRobotCacheManager.getValidBotRobotById(botId);
        Asserts.notNull(botRobot, "权限不足");
        return botRobot;
    }

    public void upBotBlocking(Long key, BiConsumer<BotRobot, String> callback) {
        Asserts.notNull(key, "参数异常");
        try {
            BaseWebSocketHandler webSocketHandler = botWebSocketHandlerMap.get(key);
            if (webSocketHandler != null && webSocketHandler.getStatus() == 0) {
                return;
            }
            Asserts.checkNull(botIdLockMap.putIfAbsent(key, true), "链接超时，请重试");
            webSocketHandler = botWebSocketHandlerMap.get(key);
            if (webSocketHandler != null && webSocketHandler.getStatus() == 0) {
                return;
            }
            if (webSocketHandler != null) {
                webSocketHandler.closeBlocking();
                botWebSocketHandlerMap.remove(key);
            }
            BaseWebSocketHandler newBotWebSocketHandler = this.newWebSocketHandle(key, callback);
            newBotWebSocketHandler.closeBlocking();
            botWebSocketHandlerMap.put(key, newBotWebSocketHandler);
        } catch (AssertException e) {
            log.warn("断言异常，message="+e.getMessage());
        } catch (Exception e) {
            log.error("异常", e);
        } finally {
            botIdLockMap.remove(key);
        }
    }

    public void downBotBlocking(BotRobot bot) {
        Long botId = bot.getId();
        Asserts.notNull(botId, "参数异常");
        try {
            Asserts.checkNull(botIdLockMap.putIfAbsent(botId, true), "链接超时，请重试");
            BaseWebSocketHandler webSocketHandler = botWebSocketHandlerMap.get(botId);
            if (webSocketHandler != null) {
                webSocketHandler.closeBlocking();
                botWebSocketHandlerMap.remove(botId);
            }
        } catch (AssertException e) {
            log.warn("断言异常，message="+e.getMessage());
        } catch (Exception e) {
            log.error("异常", e);
        } finally {
            botIdLockMap.remove(botId);
        }
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
}
