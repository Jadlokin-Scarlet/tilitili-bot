package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotConfigConstant;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotConfigManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ConfigHandle extends ExceptionRespMessageHandle {
    private final List<String> booleanTextList = Arrays.asList("yes", "no");
    public static final String autoSellFishKey = "自动卖鱼";
    public static final String autoSellRepeatFishKey = "回收重复";
    public static final String favoriteUserIdKey = "老婆QQ";
    public static final String aiSystemKey = "ai语境";
    private final BotConfigManager botConfigManager;

    @Autowired
    public ConfigHandle(BotConfigManager botConfigManager) {
        this.botConfigManager = botConfigManager;
    }

    @Override
    public String getHelpMessage(BotTask botTask, String key) {
        switch (key) {
            case autoSellFishKey: return "上钩后自动卖掉鱼，和回收重复互斥。格式：（配置 自动卖鱼 yes）";
            case autoSellRepeatFishKey: return "上钩后自动卖掉已经有的鱼，和自动卖鱼互斥。格式：（配置 回收重复 yes）";
            case favoriteUserIdKey: return "如果有需要，可以设置老婆的社交账号。格式：（配置 老婆QQ @老婆）";
            case BotConfigConstant.giveUpAdminKey: return "弃权管理员投票。格式：（配置 "+BotConfigConstant.giveUpAdminKey+" yes）";
            default: return super.getHelpMessage(botTask, key);
        }
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        List<Pair<String, String>> paramList = messageAction.getParamList();

        if (messageAction.getValue().contains(" ")) {
            String[] valueList = messageAction.getValue().split(" ");
            paramList.add(Pair.of(valueList[0], valueList[1]));
        } else {
            paramList.add(Pair.of(messageAction.getValue(), null));
        }

        List<String> respList = new ArrayList<>();

        for (Pair<String, String> param : paramList) {
            try {
                String key = param.getKey();
                switch (key) {
                    case autoSellFishKey: {
                        String autoSellFish = param.getValue();
                        if (autoSellFish != null) {
                            Asserts.isTrue(booleanTextList.contains(autoSellFish), "只能填(%s)哦", String.join(",", booleanTextList));
                            if ("yes".equals(autoSellFish)) {
                                botConfigManager.addOrUpdateUserConfig(userId, autoSellRepeatFishKey, "no");
                                respList.add("之后钓到的鱼将会自动回收喵。");
                            } else {
                                respList.add(autoSellFishKey + "关闭喵。");
                            }
                            botConfigManager.addOrUpdateUserConfig(userId, autoSellFishKey, autoSellFish);
                        }
                        break;
                    }
                    case autoSellRepeatFishKey: {
                        String autoSellRepeatFish = param.getValue();
                        if (autoSellRepeatFish != null) {
                            Asserts.isTrue(booleanTextList.contains(autoSellRepeatFish), "只能填(%s)哦", String.join(",", booleanTextList));
                            if ("yes".equals(autoSellRepeatFish)) {
                                botConfigManager.addOrUpdateUserConfig(userId, autoSellFishKey, "no");
                                respList.add("之后钓到的重复的鱼将会自动回收喵。");
                            } else {
                                respList.add(autoSellRepeatFishKey + "关闭喵。");
                            }
                            botConfigManager.addOrUpdateUserConfig(userId, autoSellRepeatFishKey, autoSellRepeatFish);
                        }
                        break;
                    }
                    case favoriteUserIdKey: {
                        List<BotUserDTO> atList = messageAction.getAtList();
                        Asserts.isTrue(atList.size() < 2, "一次配置只能@一个人哦");
                        String newValue = atList.isEmpty()? null: String.valueOf(atList.get(0).getId());
                        int result = botConfigManager.addOrUpdateUserConfig(userId, favoriteUserIdKey, newValue);
                        if (result == 1) {
                            respList.add("设置老婆QQ成功喵。");
                        } else if (result == -1) {
                            respList.add("已移除老婆QQ喵。");
                        }
                        break;
                    }
                    case aiSystemKey: {
                        String aiSystem = param.getValue();
                        if (aiSystem != null) {
                            botConfigManager.addOrUpdateUserConfig(userId, aiSystemKey, aiSystem);
                            respList.add("设置ai语境成功喵。");
                        }
                        break;
                    }
                    default: throw new AssertException(key + "是什么？");
                }
            } catch (AssertException e) {
                respList.add(e.getMessage());
            }
        }

        if (!respList.isEmpty()) {
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
        return null;
    }
}
