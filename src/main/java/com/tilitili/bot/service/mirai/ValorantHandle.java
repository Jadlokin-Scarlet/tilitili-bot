package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotUserValorantLogin;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.ValorantManager;
import com.tilitili.common.mapper.mysql.BotUserValorantLoginMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

@Component
public class ValorantHandle extends ExceptionRespMessageHandle {
    private final ValorantManager valorantManager;
    private final BotUserValorantLoginMapper botUserValorantLoginMapper;

    public ValorantHandle(ValorantManager valorantManager, BotUserValorantLoginMapper botUserValorantLoginMapper) {
        this.valorantManager = valorantManager;
        this.botUserValorantLoginMapper = botUserValorantLoginMapper;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        switch (messageAction.getKeyWithoutPrefix()) {
            case "瓦的店铺": return handleQuertEveryDayShop(messageAction);
            case "瓦罗兰特": {
                Asserts.notBlank(messageAction.getValue(), "格式错啦");
                String subKey = messageAction.getValue().split("")[0];
                switch (subKey) {
                    case "每日商店": return handleQuertEveryDayShop(messageAction);
                    case "邮箱验证": return handleEmailVery(messageAction);
                    default: throw new AssertException("格式错啦");
                }
            }
            default: throw new AssertException("格式错啦");
        }
    }

    private BotMessage handleEmailVery(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        String value = messageAction.getValue();
        Asserts.isTrue(value.contains(" "), "格式错啦(验证码)");
        String veryCode = value.substring(value.indexOf(" "));

        valorantManager.emailVery(botUser.getId(), veryCode);
        return BotMessage.emptyMessage();
    }

    private BotMessage handleQuertEveryDayShop(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();

        BotUserValorantLogin valorantLogin = botUserValorantLoginMapper.getBotUserValorantLoginByUserId(botUser.getId());
        Asserts.notNull(valorantLogin, "未绑定账号");

        String everyDayShopImageUrl = valorantManager.getEveryDayShop(botUser.getId());
        return BotMessage.simpleImageMessage(everyDayShopImageUrl);
    }
}
