package com.tilitili.bot.socket;

import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class BotWebSocketHandler extends BaseWebSocketHandler {
    protected final Long botId;
    protected final BotService botService;
    protected final SendMessageManager sendMessageManager;
    protected final BotRobotCacheManager botRobotCacheManager;

    public BotWebSocketHandler(URI serverUri, BotRobot bot, BotService botService, SendMessageManager sendMessageManager, BotRobotCacheManager botRobotCacheManager) {
        super(serverUri);
        this.botId = bot.getId();
        this.botService = botService;
        this.sendMessageManager = sendMessageManager;
        this.botRobotCacheManager = botRobotCacheManager;
    }

    @Override
    public void handleTextMessage(String message) {
        BotRobot bot = botRobotCacheManager.getValidBotRobotById(botId);
        Asserts.notNull(bot, "bot权限不足");
        if (!message.contains("heartbeat")) {
            log.info("Message Received bot={} message={}", bot.getName(), message);
        }
        botService.syncHandleMessage(bot, message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
//        BotMessage botMessage = BotMessage.simpleListMessage(Lists.newArrayList(
//                BotMessageChain.ofPlain("连接已断开，请检查。")
//        ));
//        if (BotRobotConstant.TYPE_GOCQ.equals(bot.getType())) {
//            botMessage.setSenderId(3777L);
//            sendMessageManager.sendMessage(botMessage);
//        } else if (BotRobotConstant.TYPE_MIRAI.equals(bot.getType())) {
//            botMessage.setSenderId(4380L);
//            sendMessageManager.sendMessage(botMessage);
//        }
        super.onClose(code, reason, remote);
    }
}
