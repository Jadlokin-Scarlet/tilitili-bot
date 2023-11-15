package com.tilitili.bot.socket;

import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.CollectionUtils;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class WebSocketFactory implements ApplicationListener<ContextClosedEvent> {
    private final Map<Long, List<BaseWebSocketHandler>> botWebSocketHandlerMap;
//    private final Long shortUrlWebSocketKey = -1L;
    private final ConcurrentHashMap<Long, Boolean> botIdLockMap = new ConcurrentHashMap<>();
    private final BotManager botManager;
    private final BotRobotCacheManager botRobotCacheManager;


    public WebSocketFactory(BotManager botManager, BotRobotCacheManager botRobotCacheManager) {
        this.botManager = botManager;
        this.botRobotCacheManager = botRobotCacheManager;
        this.botWebSocketHandlerMap = new HashMap<>();
    }

    public BotWebSocketHandler getWebSocketOrNull(BotRobot bot) {
        return (BotWebSocketHandler) botWebSocketHandlerMap.get(bot.getId());
    }

//    // 短连接Websocket只会有一个
//    public ShortUrlWebSocketHandler getShortUrlWebSocketHandler() {
//        List<BaseWebSocketHandler> webSocketHandlerList = botWebSocketHandlerMap.get(shortUrlWebSocketKey);
//        if (CollectionUtils.isNotEmpty(webSocketHandlerList) && webSocketHandlerList.get(0).getStatus() == 0) {
//            return (ShortUrlWebSocketHandler) botWebSocketHandlerMap.get(shortUrlWebSocketKey);
//        }
//        this.upBotBlocking(shortUrlWebSocketKey, null);
//        return (ShortUrlWebSocketHandler) botWebSocketHandlerMap.get(shortUrlWebSocketKey).get(0);
//    }

    private List<BaseWebSocketHandler> newWebSocketHandle(Long key, BiConsumer<BotRobot, String> callback) {
        try {
//            if (this.shortUrlWebSocketKey.equals(key)) {
//                return new ShortUrlWebSocketHandler(redisCache);
//            }
            BotRobot bot = botRobotCacheManager.getValidBotRobotById(key);
            Asserts.notNull(bot, "权限不足");
            String wsUrl = botManager.getWebSocketUrl(bot);
            Asserts.notNull(wsUrl, "%s获取ws地址异常", bot.getName());
            switch (bot.getType()) {
                case BotRobotConstant.TYPE_MIRAI:
                case BotRobotConstant.TYPE_GOCQ: return Collections.singletonList(new BotWebSocketHandler(new URI(wsUrl), bot, this, callback));
                case BotRobotConstant.TYPE_KOOK: return Collections.singletonList(new KookWebSocketHandler(new URI(wsUrl), bot, this, callback));
                case BotRobotConstant.TYPE_QQ_GUILD: return Arrays.asList(
//                        new QQGuildWebSocketHandler(new URI(wsUrl), bot, this, callback, botManager.getAccessToken(bot, "group"))
                        new QQGuildWebSocketHandler(new URI(wsUrl), bot, this, callback, botManager.getAccessToken(bot, "guild"))
                );
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
            List<BaseWebSocketHandler> webSocketHandlerList = botWebSocketHandlerMap.get(key);
            if (CollectionUtils.isNotEmpty(webSocketHandlerList) && webSocketHandlerList.stream().allMatch(StreamUtil.isEqual(BaseWebSocketHandler::getStatus, 0))) {
                return;
            }
            Asserts.checkNull(botIdLockMap.putIfAbsent(key, true), "链接超时，请重试");
            log.info("尝试连接ws key="+key);
            webSocketHandlerList = botWebSocketHandlerMap.get(key);
            if (CollectionUtils.isNotEmpty(webSocketHandlerList) && webSocketHandlerList.stream().allMatch(StreamUtil.isEqual(BaseWebSocketHandler::getStatus, 0))) {
                return;
            }
            if (CollectionUtils.isNotEmpty(webSocketHandlerList)) {
                for (BaseWebSocketHandler webSocketHandler : webSocketHandlerList) {
                    webSocketHandler.closeBlocking();
                }
                botWebSocketHandlerMap.remove(key);
            }
            List<BaseWebSocketHandler> newBotWebSocketHandlerList = this.newWebSocketHandle(key, callback);
            for (BaseWebSocketHandler newBotWebSocketHandler : newBotWebSocketHandlerList) {
                newBotWebSocketHandler.connectBlocking();
            }
            botWebSocketHandlerMap.put(key, newBotWebSocketHandlerList);
        } catch (AssertException e) {
            log.warn("断言异常，message="+e.getMessage());
        } catch (Exception e) {
            log.error("异常", e);
        } finally {
            log.info("连接ws结束 key="+key);
            botIdLockMap.remove(key);
        }
    }

    public void downBotBlocking(BotRobot bot) {
        Long botId = bot.getId();
        Asserts.notNull(botId, "参数异常");
        try {
            Asserts.checkNull(botIdLockMap.putIfAbsent(botId, true), "链接超时，请重试");
            List<BaseWebSocketHandler> webSocketHandlerList = botWebSocketHandlerMap.get(botId);
            if (webSocketHandlerList != null) {
                for (BaseWebSocketHandler webSocketHandler : webSocketHandlerList) {
                    webSocketHandler.closeBlocking();
                }
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
            for (List<BaseWebSocketHandler> botWebSocketHandlerList : botWebSocketHandlerMap.values()) {
                for (BaseWebSocketHandler botWebSocketHandler : botWebSocketHandlerList) {
                    botWebSocketHandler.closeBlocking();
                }
            }
        } catch (InterruptedException e) {
            log.error("优雅停机异常");
        }
    }
}
