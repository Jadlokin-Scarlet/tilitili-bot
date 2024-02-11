package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserConfigManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ConfigHandle extends ExceptionRespMessageHandle {
    private final List<String> booleanTextList = Arrays.asList("yes", "no");
    public static final String autoSellFishKey = "自动卖鱼";
    public static final String autoSellRepeatFishKey = "回收重复";
    public static final String favoriteUserIdKey = "老婆QQ";
    public static final String aiSystemKey = "ai语境";
    private final BotUserConfigManager botUserConfigManager;

    @Autowired
    public ConfigHandle(BotUserConfigManager botUserConfigManager) {
        this.botUserConfigManager = botUserConfigManager;
    }

    @Override
    public String getHelpMessage(BotTask botTask, String key) {
        switch (key) {
            case autoSellFishKey: return "上钩后自动卖掉鱼，和回收重复互斥。格式：（配置 自动卖鱼 yes）";
            case autoSellRepeatFishKey: return "上钩后自动卖掉已经有的鱼，和自动卖鱼互斥。格式：（配置 回收重复 yes）";
            case favoriteUserIdKey: return "如果有需要，可以设置老婆的社交账号。格式：（配置 老婆QQ @老婆）";
            default: return super.getHelpMessage(botTask, key);
        }
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
                    Asserts.isTrue(botUserConfigManager.getBooleanConfigCache(userId, autoSellRepeatFishKey), "你已经设置了(%s)", autoSellRepeatFishKey);
                    respList.add("之后钓到的鱼将会自动回收喵。");
                } else {
                    respList.add(autoSellFishKey + "关闭喵。");
                }
                botUserConfigManager.addOrUpdateConfig(userId, autoSellFishKey, autoSellFish);
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        try {
            String autoSellRepeatFish = paramMap.get(autoSellRepeatFishKey);
            if (autoSellRepeatFish != null) {
                Asserts.isTrue(booleanTextList.contains(autoSellRepeatFish), "只能填(%s)哦", String.join(",", booleanTextList));
                if ("yes".equals(autoSellRepeatFish)) {
                    Asserts.isTrue(botUserConfigManager.getBooleanConfigCache(userId, autoSellRepeatFishKey), "你已经设置了(%s)", autoSellFishKey);
                    respList.add("之后钓到的重复的鱼将会自动回收喵。");
                } else {
                    respList.add(autoSellRepeatFishKey + "关闭喵。");
                }
                botUserConfigManager.addOrUpdateConfig(userId, autoSellRepeatFishKey, autoSellRepeatFish);
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        try {
            if (paramMap.containsKey(favoriteUserIdKey)) {
                List<BotUserDTO> atList = messageAction.getAtList();
                Asserts.isTrue(atList.size() < 2, "一次配置只能@一个人哦");
                String newValue = atList.isEmpty()? null: String.valueOf(atList.get(0).getId());
                int result = botUserConfigManager.addOrUpdateConfig(userId, favoriteUserIdKey, newValue);
                if (result == 1) {
                    respList.add("设置老婆QQ成功喵。");
                } else if (result == -1) {
                    respList.add("已移除老婆QQ喵。");
                }
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        try {
            String aiSystem = paramMap.get(aiSystemKey);
            if (aiSystem != null) {
                botUserConfigManager.addOrUpdateConfig(userId, aiSystemKey, aiSystem);
                respList.add("设置ai语境成功喵。");
            }
        } catch (AssertException e) {
            respList.add(e.getMessage());
        }

        if (!respList.isEmpty()) {
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
        return null;
    }
}
