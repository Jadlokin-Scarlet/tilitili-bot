package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotUserConfig;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserConfigMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ConfigHandle extends ExceptionRespMessageHandle {
    private final List<String> booleanTextList = Arrays.asList("yes", "no");
    public static final String autoSellFishKey = "自动卖鱼";
    public static final String autoSellRepeatFishKey = "回收重复";
    public static final String favoriteUserIdKey = "老婆QQ";
    private final BotUserConfigMapper botUserConfigMapper;
    private final BotUserManager botUserManager;

    @Autowired
    public ConfigHandle(BotUserConfigMapper botUserConfigMapper, BotUserManager botUserManager) {
        this.botUserConfigMapper = botUserConfigMapper;
        this.botUserManager = botUserManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        Map<String, String> paramMap = messageAction.getParamMap();

        if (messageAction.getValue().contains(" ")) {
            String[] valueList = messageAction.getValue().split(" ");
            paramMap.put(valueList[0], valueList[1]);
        } else {
            paramMap.put(messageAction.getValue(), null);
        }

        List<String> respList = new ArrayList<>();

        try {
            String autoSellFish = paramMap.get(autoSellFishKey);
            if (autoSellFish != null) {
                Asserts.isTrue(booleanTextList.contains(autoSellFish), "只能填(%s)哦", String.join(",", booleanTextList));
                if ("yes".equals(autoSellFish)) {
                    Asserts.notEquals(botUserConfigMapper.getValueByUserIdAndKey(userId, autoSellRepeatFishKey), "yes", "你已经设置了(%s)", autoSellRepeatFishKey);
                    respList.add("之后钓到的鱼将会自动回收喵。");
                } else {
                    respList.add(autoSellFishKey + "关闭喵。");
                }
                this.addOrUpdateUserConfig(userId, autoSellFishKey, autoSellFish);
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        try {
            String autoSellRepeatFish = paramMap.get(autoSellRepeatFishKey);
            if (autoSellRepeatFish != null) {
                Asserts.isTrue(booleanTextList.contains(autoSellRepeatFish), "只能填(%s)哦", String.join(",", booleanTextList));
                if ("yes".equals(autoSellRepeatFish)) {
                    Asserts.notEquals(botUserConfigMapper.getValueByUserIdAndKey(userId, autoSellFishKey), "yes", "你已经设置了(%s)", autoSellFishKey);
                    respList.add("之后钓到的重复的鱼将会自动回收喵。");
                } else {
                    respList.add(autoSellRepeatFishKey + "关闭喵。");
                }
                this.addOrUpdateUserConfig(userId, autoSellRepeatFishKey, autoSellRepeatFish);
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        try {
            if (paramMap.containsKey(favoriteUserIdKey)) {
                List<Long> atList = messageAction.getAtList();
                Asserts.isTrue(atList.size() < 2, "一次配置只能@一个人哦");
                if (!atList.isEmpty()) {
                    BotUserDTO favoriteUser = botUserManager.getBotUserByIdWithParent(atList.get(0));
                    this.addOrUpdateUserConfig(userId, favoriteUserIdKey, String.valueOf(favoriteUser.getId()));
                    respList.add("设置老婆QQ成功喵。");
                } else {
                    this.deleteUserConfig(userId, favoriteUserIdKey);
                    respList.add("已移除老婆QQ喵。");
                }
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        if (!respList.isEmpty()) {
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
        return null;
    }

    private void deleteUserConfig(Long userId, String key) {
        BotUserConfig userConfig = botUserConfigMapper.getBotUserConfigByUserIdAndKey(userId, key);
        if (userConfig != null) {
            botUserConfigMapper.deleteBotUserConfigByPrimary(userConfig.getId());
        }
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
