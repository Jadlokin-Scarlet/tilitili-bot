package com.tilitili.bot.socket.handle;

import com.google.gson.JsonElement;
import com.tilitili.bot.entity.McPanelWsData;
import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class McPanelWebSocketHandler extends BotWebSocketHandler {
    private final BotService botService;

    public McPanelWebSocketHandler(URI serverUri, BotRobot bot, BotService botService, BotRobotCacheManager botRobotCacheManager) {
        super(serverUri, bot, botService, botRobotCacheManager);
        this.botService = botService;
    }

    @Override
    protected void handleBotMessage(BotRobot bot, String message) {
        try {
            log.info("Message Received bot={} message={}", bot.getName(), message);
            McPanelWsData mcPanelWsData = Gsons.fromJson(message, McPanelWsData.class);
            switch (mcPanelWsData.getType()) {
                case "return": return;
                case "broadcast": {
                    List<JsonElement> list = mcPanelWsData.getData().getAsJsonArray().asList();
                    List<String> broadList = list.stream().map(JsonElement::getAsString).collect(Collectors.toList());
                    for (String broad : broadList) {
                        botService.syncHandleMessage(bot, broad);
                    }
                }
                default: {
                    log.info("记录未知类型{}", mcPanelWsData.getType());
                }
            }
        } catch (Exception e) {
            log.error("处理websocket异常, message="+message, e);
        }
    }

}
