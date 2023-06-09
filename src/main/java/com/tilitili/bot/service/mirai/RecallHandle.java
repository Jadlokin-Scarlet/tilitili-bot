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
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RecallHandle extends ExceptionRespMessageHandle {

    private final BotManager botManager;
    private final BotRobotMapper botRobotMapper;
    private final BotSenderMapper botSenderMapper;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;

    @Autowired
    public RecallHandle(BotManager botManager, BotRobotMapper botRobotMapper, BotSenderMapper botSenderMapper, BotSendMessageRecordMapper botSendMessageRecordMapper) {
        this.botManager = botManager;
        this.botRobotMapper = botRobotMapper;
        this.botSenderMapper = botSenderMapper;
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
                BotSender otherBotSender = botSenderMapper.getBotSenderById(sendMessage.getSenderId());
                BotRobot otherBot = botRobotMapper.getValidBotRobotById(otherBotSender.getBot());
                Asserts.notNull(otherBot, "啊嘞，不对劲");
                botManager.recallMessage(otherBot, otherBotSender, sendMessage.getMessageId());
            }
            return BotMessage.simpleTextMessage("搞定");
        } else {
            List<BotSendMessageRecord> recordList = botSendMessageRecordMapper.getBotSendMessageRecordByCondition(new BotSendMessageRecordQuery().setSenderId(botSender.getId()).setPageNo(1).setPageSize(1).setSorter("create_time").setSorted("desc"));
            for (BotSendMessageRecord messageRecord : recordList) {
                botManager.recallMessage(bot, botSender, messageRecord.getMessageId());
            }
//            Long bot = botSender.getBot();
//            botManager.recallMessage(BotEnum.getBotById(bot), botSender, session.get(BotService.lastMessageIdKey));
            return BotMessage.emptyMessage();
        }

//        if (Objects.equals(qq, MASTER_QQ)) {
//            if (pid != null) {
//                List<PixivImage> pixivImageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setPid(pid));
//                for (PixivImage pixivImage : pixivImageList) {
//                    String messageId = pixivImage.getMessageId();
//                    if (messageId != null) {
//                        BotSendMessageRecord botSendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(messageId);
//                        BotSender botSender = botSenderMapper.getBotSenderById(botSendMessageRecord.getSenderId());
//                        botManager.recallMessage(messageId, botSender.getBot());
//                        return BotMessage.simpleTextMessage("搞定");
//                    }
//                }
//                return null;
//            }

//            String messageIdStr = (String) redisCache.getValue(PixivHandle.messageIdKey);
//            if (! isBlank(messageIdStr)) {
//                BotSendMessageRecord botSendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(messageIdStr);
//                BotSender botSender = botSenderMapper.getBotSenderById(botSendMessageRecord.getSenderId());
//                botManager.recallMessage(botSendMessageRecord.getMessageId(), botSender.getBot());
//                return BotMessage.simpleTextMessage("搞定");
//            }
//        }
//        return null;
    }
}
