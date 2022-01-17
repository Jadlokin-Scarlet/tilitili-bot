package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;


public class ConfigHandle extends ExceptionRespMessageHandle {
    private final RedisCache redisCache;

    @Autowired
    public ConfigHandle(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        String stKey = messageAction.getParam("色图开关");
        if (stKey != null) {
            redisCache.setValue("色图开关", stKey);
        }
        return null;
    }
}
