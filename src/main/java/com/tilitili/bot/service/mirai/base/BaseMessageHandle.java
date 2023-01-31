package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;

import java.util.List;

public interface BaseMessageHandle {
    List<BotMessage> handleMessageNew(BotMessageAction messageAction) throws Exception;

	String isThisTask(BotMessageAction botMessageAction);
}
