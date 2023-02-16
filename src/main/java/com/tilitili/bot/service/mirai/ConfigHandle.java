package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ConfigHandle extends ExceptionRespMessageHandle {
    private final List<String> booleanTextList = Arrays.asList("yes", "no");
    public static final String autoSellFishKey = "autoSellFish-";
    public static final String autoSellRepeatFishKey = "autoSellRepeatFish-";
    private final RedisCache redisCache;

    @Autowired
    public ConfigHandle(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        String autoSellFish = messageAction.getParam("自动卖鱼");
        if (autoSellFish != null) {
            Asserts.notEquals(autoSellRepeatFishKey + userId, "yes", "你已经设置了(回收重复)");
            Asserts.isTrue(booleanTextList.contains(autoSellFish), "只能填(%s)哦", String.join(",", booleanTextList));
            redisCache.setValue(autoSellFishKey + userId, autoSellFish);
            return BotMessage.simpleTextMessage("之后掉到的鱼将会自动回收喵。");
        }
        String autoSellRepeatFish = messageAction.getParam("回收重复");
        if (autoSellRepeatFish != null) {
            Asserts.notEquals(autoSellFishKey + userId, "yes", "你已经设置了(自动卖鱼)");
            Asserts.isTrue(booleanTextList.contains(autoSellRepeatFish), "只能填(%s)哦", String.join(",", booleanTextList));
            redisCache.setValue(autoSellRepeatFishKey + userId, autoSellRepeatFish);
            return BotMessage.simpleTextMessage("之后掉到的重复的鱼将会自动回收喵。");
        }
        return null;
    }

    public String getConfigByUser(BotUserDTO botUser, String key) {
        return (String) redisCache.getValue(key + botUser.getId());
    }
}
