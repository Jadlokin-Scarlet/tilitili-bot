package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;

public abstract class ExceptionRespMessageToSenderHandle extends ExceptionRespMessageHandleAdapt {
	@Override
	public BotMessage handleAssertException(BotMessageAction messageAction, AssertException e) {
		return BotMessage.simpleTextMessage(e.getMessage()).setQuote(messageAction.getMessageId());
	}
}
