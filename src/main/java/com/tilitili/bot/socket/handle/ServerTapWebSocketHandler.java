package com.tilitili.bot.socket.handle;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.HttpRequestDTO;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

@Slf4j
public class ServerTapWebSocketHandler extends BotWebSocketHandler {
    private final BotService botService;
    private final BotManager botManager;

    public ServerTapWebSocketHandler(HttpRequestDTO wsRequest, BotRobot bot, BotManager botManager, BotService botService, BotRobotCacheManager botRobotCacheManager) throws URISyntaxException {
        super(wsRequest, bot, botService, botRobotCacheManager);
        this.botService = botService;
        this.botManager = botManager;
    }

    @Override
    protected void handleBotMessage(BotRobot bot, String message) {
        try {
//            log.info("Message Received bot={} message={}", bot.getName(), message);
            botService.syncHandleMessage(bot, message);
        } catch (Exception e) {
            log.error("处理websocket异常, message="+message, e);
        }
    }

}
