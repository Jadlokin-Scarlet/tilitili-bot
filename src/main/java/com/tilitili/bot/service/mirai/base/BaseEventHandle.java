package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.view.bot.BotMessage;

public interface BaseEventHandle {
    String getEventType();
    BotMessage handleEvent(BotEnum bot, BotMessage botMessage) throws Exception;
}
