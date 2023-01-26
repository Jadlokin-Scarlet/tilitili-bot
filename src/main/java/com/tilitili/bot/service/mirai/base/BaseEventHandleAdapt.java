package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.view.bot.BotMessage;

public abstract class BaseEventHandleAdapt implements BaseEventHandle {
	final String eventType;

	protected BaseEventHandleAdapt(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public String getEventType() {
		return this.eventType;
	}

	@Override
	public abstract BotMessage handleEvent(BotEnum bot, BotMessage botMessage) throws Exception;
}
