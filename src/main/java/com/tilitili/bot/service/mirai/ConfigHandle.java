package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotUserConfig;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotUserConfigMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConfigHandle extends ExceptionRespMessageHandle {
    private final List<String> booleanTextList = Arrays.asList("yes", "no");
    public static final String autoSellFishKey = "自动卖鱼";
    public static final String autoSellRepeatFishKey = "回收重复";
    private final BotUserConfigMapper botUserConfigMapper;

    @Autowired
    public ConfigHandle(BotUserConfigMapper botUserConfigMapper) {
        this.botUserConfigMapper = botUserConfigMapper;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();

        String value = messageAction.getValue();
        Map<String, String> valueParamMap = new HashMap<>();
        if (value.contains(" ")) {
            String[] valueList = value.split(" ");
            valueParamMap.put(valueList[0], valueList[1]);
        }

        List<String> respList = new ArrayList<>();

        String autoSellFish = messageAction.getParamOrDefault(autoSellFishKey, valueParamMap.get(autoSellFishKey));
        if (autoSellFish != null) {
            Asserts.notEquals(botUserConfigMapper.getValueByUserIdAndKey(userId, autoSellRepeatFishKey), "yes", "你已经设置了(%s)", autoSellRepeatFishKey);
            Asserts.isTrue(booleanTextList.contains(autoSellFish), "只能填(%s)哦", String.join(",", booleanTextList));
            this.addOrUpdateUserConfig(userId, autoSellFishKey, autoSellFish);
            respList.add("之后掉到的鱼将会自动回收喵。");
        }
        String autoSellRepeatFish = messageAction.getParamOrDefault(autoSellRepeatFishKey, valueParamMap.get(autoSellRepeatFishKey));
        if (autoSellRepeatFish != null) {
            Asserts.notEquals(botUserConfigMapper.getValueByUserIdAndKey(userId, autoSellFishKey), "yes", "你已经设置了(%s)", autoSellFishKey);
            Asserts.isTrue(booleanTextList.contains(autoSellRepeatFish), "只能填(%s)哦", String.join(",", booleanTextList));
            this.addOrUpdateUserConfig(userId, autoSellRepeatFishKey, autoSellRepeatFish);
            respList.add("之后掉到的重复的鱼将会自动回收喵。");
        }

        if (!respList.isEmpty()) {
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
        return null;
    }

    private void addOrUpdateUserConfig(Long userId, String key, String value) {
        BotUserConfig userConfig = botUserConfigMapper.getBotUserConfigByUserIdAndKey(userId, key);
        if (userConfig == null) {
            botUserConfigMapper.addBotUserConfigSelective(new BotUserConfig().setUserId(userId).setKey(key).setValue(value));
            return;
        }
        if (!Objects.equals(userConfig.getValue(), value)) {
            botUserConfigMapper.updateBotUserConfigSelective(new BotUserConfig().setId(userConfig.getId()).setValue(value));
        }
    }
}
