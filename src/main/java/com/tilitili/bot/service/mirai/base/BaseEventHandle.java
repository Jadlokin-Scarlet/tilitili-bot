package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEnum;

public interface BaseEventHandle {
    void handleEventStr(BotEnum bot, String eventMessage) throws Exception;
}
