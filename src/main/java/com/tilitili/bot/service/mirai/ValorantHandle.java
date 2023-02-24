package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotUserValorantLogin;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
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
        BotUserDTO botUser = messageAction.getBotUser();

        BotUserValorantLogin valorantLogin = botUserValorantLoginMapper.getBotUserValorantLoginByUserId(botUser.getId());
        Asserts.notNull(valorantLogin, "未绑定账号");

        String everyDayShopImageUrl = valorantManager.getEveryDayShop(botUser.getId());
        return BotMessage.simpleImageMessage(everyDayShopImageUrl);
    }
}
