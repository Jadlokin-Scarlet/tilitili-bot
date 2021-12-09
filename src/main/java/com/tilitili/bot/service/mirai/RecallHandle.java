package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.mirai.MiraiMessage;
import com.tilitili.common.entity.mirai.Sender;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.jsoup.helper.StringUtil.isBlank;

@Component
public class RecallHandle implements BaseMessageHandle {
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
        return MessageHandleEnum.RecallHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) throws Exception {
        Sender sender = request.getMessage().getSender();
        String pid = request.getParamOrDefault("pid", request.getTitleValue());
        MiraiMessage result = new MiraiMessage();

        if (sender.getId().equals(MASTER_QQ)) {
            if (pid == null) {
                String messageIdStr = (String) redisCache.getValue(PixivHandle.messageIdKey);
                if (! isBlank(messageIdStr)) {
                    int messageId = Integer.parseInt(messageIdStr);
                    miraiManager.recallMessage(messageId);
                    return result;
                }
            } else {
                List<PixivImage> pixivImageList = pixivImageMapper.getPixivImageByCondition(new PixivImage().setPid(pid));
                for (PixivImage pixivImage : pixivImageList) {
                    Integer messageId = pixivImage.getMessageId();
                    if (messageId != null) {
                        miraiManager.recallMessage(messageId);
                        return result;
                    }
                }
            }


        }
        return null;
    }
}
