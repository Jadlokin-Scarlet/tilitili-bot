package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;

public abstract class BaseMessageHandleAdapt implements BaseMessageHandle {

	@Override
	public abstract BotMessage handleMessage(BotMessageAction messageAction) throws Exception;

	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		return null;
	}
}
