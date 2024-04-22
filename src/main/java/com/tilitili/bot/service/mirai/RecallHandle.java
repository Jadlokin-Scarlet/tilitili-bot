package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotSendMessageRecordQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RecallHandle extends ExceptionRespMessageHandle {

    private final BotManager botManager;
    private final BotRobotCacheManager botRobotCacheManager;
    private final BotSenderCacheManager botSenderCacheManager;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;

    @Autowired
    public RecallHandle(BotManager botManager, BotRobotCacheManager botRobotCacheManager, BotSenderCacheManager botSenderCacheManager, BotSendMessageRecordMapper botSendMessageRecordMapper) {
        this.botManager = botManager;
        this.botRobotCacheManager = botRobotCacheManager;
        this.botSenderCacheManager = botSenderCacheManager;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        BotSessionService.MiraiSession session = messageAction.getSession();
        BotRobot bot = messageAction.getBot();
        BotSender botSender = messageAction.getBotSender();
//        Long qq = messageAction.getBotMessage().getQq();
//        String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
        String all = messageAction.getParamOrDefault("all", messageAction.getValue());
        String quoteMessageId = messageAction.getQuoteMessageId();
        boolean recallAll = Objects.equals(all, "1");

        if (quoteMessageId != null) {
            botManager.recallMessage(bot, botSender, quoteMessageId);
            return BotMessage.emptyMessage();
        } else if (recallAll) {
            List<BotSendMessageRecord> sendMessageList = botSendMessageRecordMapper.getNewBotsendMessageList();
            for (BotSendMessageRecord sendMessage : sendMessageList) {
                BotSender otherBotSender = botSenderCacheManager.getBotSenderById(sendMessage.getSenderId());
                BotRobot otherBot = botRobotCacheManager.getValidBotRobotById(otherBotSender.getSendBot());
                Asserts.notNull(otherBot, "啊嘞，不对劲");
                botManager.recallMessage(otherBot, otherBotSender, sendMessage.getMessageId());
            }
            return BotMessage.simpleTextMessage("搞定");
        } else {
            List<BotSendMessageRecord> recordList = botSendMessageRecordMapper.getBotSendMessageRecordByCondition(new BotSendMessageRecordQuery().setSenderId(botSender.getId()).setPageNo(1).setPageSize(1).setSorter("create_time").setSorted("desc"));
            for (BotSendMessageRecord messageRecord : recordList) {
                botManager.recallMessage(bot, botSender, messageRecord.getMessageId());
            }
            return BotMessage.emptyMessage();
        }
    }
}
