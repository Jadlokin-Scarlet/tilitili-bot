package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.BotMessage;

import java.util.List;

public interface BaseEventHandle {
    String getEventType();
    List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) throws Exception;
}
