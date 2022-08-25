package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;

public abstract class ExceptionRespMessageHandleAdapt extends BaseMessageHandleAdapt {
	public abstract BotMessage handleAssertException(BotMessageAction messageAction, AssertException e);
}
