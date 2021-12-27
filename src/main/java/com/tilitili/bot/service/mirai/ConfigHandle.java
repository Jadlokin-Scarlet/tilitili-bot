package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;


public class ConfigHandle implements BaseMessageHandle {


    private final RedisCache redisCache;

    @Autowired
    public ConfigHandle(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.ConfigHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) throws Exception {
        String stKey = request.getParam("色图开关");
        if (stKey != null) {
            redisCache.setValue("色图开关", stKey);
        }
        return null;
    }
}
