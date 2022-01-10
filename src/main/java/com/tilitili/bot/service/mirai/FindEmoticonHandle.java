package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.DbbqbManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FindEmoticonHandle extends ExceptionRespMessageHandle{
    private final String emoticonKey = "emoticon";
    private final DbbqbManager dbbqbManager;
    private final RedisCache redisCache;

    @Autowired
    public FindEmoticonHandle(DbbqbManager dbbqbManager, RedisCache redisCache) {
        this.dbbqbManager = dbbqbManager;
        this.redisCache = redisCache;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.FIND_EMOTICON_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String tag = messageAction.getParamOrDefault("tag", messageAction.getValue());
        Asserts.notBlank(tag, "格式错啦(tag)");

        BotMessage botMessage = messageAction.getBotMessage();
        Long qq = botMessage.getQq();
        String tinyId = botMessage.getTinyId();
        String sender = qq != null? String.valueOf(qq) : tinyId;

        Long start = redisCache.increment(emoticonKey, sender) - 1;

        List<String> imgList = dbbqbManager.searchEmoticon(tag, start);
        Asserts.notEmpty(imgList, "没找到表情包");

        String img = imgList.get(0);
        return BotMessage.simpleImageMessage(img);
    }
}
