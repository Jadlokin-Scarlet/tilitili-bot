package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;

public interface BaseMessageHandle {
    BotMessage handleMessage(BotMessageAction messageAction) throws Exception;

	String isThisTask(BotMessageAction botMessageAction);
}
