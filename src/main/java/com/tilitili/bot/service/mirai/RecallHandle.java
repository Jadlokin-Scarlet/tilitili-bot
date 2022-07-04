package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.pixiv.PixivHandle;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.jsoup.helper.StringUtil.isBlank;

@Component
public class RecallHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;

    private final RedisCache redisCache;
    private final PixivImageMapper pixivImageMapper;
    private final BotManager botManager;
    private final BotSenderMapper botSenderMapper;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;

    @Autowired
    public RecallHandle(RedisCache redisCache, PixivImageMapper pixivImageMapper, BotManager botManager, BotSenderMapper botSenderMapper, BotSendMessageRecordMapper botSendMessageRecordMapper) {
        this.redisCache = redisCache;
        this.pixivImageMapper = pixivImageMapper;
        this.botManager = botManager;
        this.botSenderMapper = botSenderMapper;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        Long qq = messageAction.getBotMessage().getQq();
        String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
        String all = messageAction.getParamOrDefault("all", messageAction.getValue());
        String quoteMessageId = messageAction.getQuoteMessageId();
        boolean recallAll = Objects.equals(all, "1");

        if (Objects.equals(qq, MASTER_QQ)) {
            if (quoteMessageId != null) {
                botManager.recallMessage(quoteMessageId, messageAction.getBotMessage().getSendType());
                return BotMessage.simpleTextMessage("搞定");
            }

            if (recallAll) {
                List<BotSendMessageRecord> sendMessageList = botSendMessageRecordMapper.getNewBotsendMessageList();
                for (BotSendMessageRecord sendMessage : sendMessageList) {
                    BotSender botSender = botSenderMapper.getBotSenderById(sendMessage.getSenderId());
                    botManager.recallMessage(sendMessage.getMessageId(), botSender.getSendType());
                }
                return BotMessage.simpleTextMessage("搞定");
            }

            if (pid != null) {
                List<PixivImage> pixivImageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setPid(pid));
                for (PixivImage pixivImage : pixivImageList) {
                    String messageId = pixivImage.getMessageId();
                    if (messageId != null) {
                        BotSendMessageRecord botSendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(messageId);
                        BotSender botSender = botSenderMapper.getBotSenderById(botSendMessageRecord.getSenderId());
                        botManager.recallMessage(messageId, botSender.getSendType());
                        return BotMessage.simpleTextMessage("搞定");
                    }
                }
                return null;
            }

            String messageIdStr = (String) redisCache.getValue(PixivHandle.messageIdKey);
            if (! isBlank(messageIdStr)) {
                BotSendMessageRecord botSendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(messageIdStr);
                BotSender botSender = botSenderMapper.getBotSenderById(botSendMessageRecord.getSenderId());
                botManager.recallMessage(botSendMessageRecord.getMessageId(), botSender.getSendType());
                return BotMessage.simpleTextMessage("搞定");
            }
        }
        return null;
    }
}
