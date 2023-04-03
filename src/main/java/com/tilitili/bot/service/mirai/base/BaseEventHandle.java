package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.BotMessage;

public interface BaseEventHandle {
    String getEventType();
    BotMessage handleEvent(BotRobot bot, BotMessage botMessage) throws Exception;
}
