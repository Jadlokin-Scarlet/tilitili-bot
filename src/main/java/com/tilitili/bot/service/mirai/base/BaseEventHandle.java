package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEmum;

public interface BaseEventHandle {
    void handleEventStr(BotEmum bot, String eventMessage) throws Exception;
}
