package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
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

    private final MiraiManager miraiManager;
    private final RedisCache redisCache;
    private final PixivImageMapper pixivImageMapper;

    @Autowired
    public RecallHandle(MiraiManager miraiManager, RedisCache redisCache, PixivImageMapper pixivImageMapper) {
        this.miraiManager = miraiManager;
        this.redisCache = redisCache;
        this.pixivImageMapper = pixivImageMapper;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.RECALL_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        Long qq = messageAction.getBotMessage().getQq();
        String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());

        if (Objects.equals(qq, MASTER_QQ)) {
            if (pid == null) {
                String messageIdStr = (String) redisCache.getValue(PixivHandle.messageIdKey);
                if (! isBlank(messageIdStr)) {
                    int messageId = Integer.parseInt(messageIdStr);
                    miraiManager.recallMessage(messageId);
                    return BotMessage.simpleTextMessage("搞定");
                }
            } else {
                List<PixivImage> pixivImageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setPid(pid));
                for (PixivImage pixivImage : pixivImageList) {
                    Integer messageId = pixivImage.getMessageId();
                    if (messageId != null) {
                        miraiManager.recallMessage(messageId);
                        return BotMessage.simpleTextMessage("搞定");
                    }
                }
            }
        }
        return null;
    }
}
