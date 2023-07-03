package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.BotMessage;

import java.util.Collections;
import java.util.List;

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
	public List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) throws Exception {
		BotMessage resp = this.handleEvent(bot, botMessage);
		if (resp == null) return null;
		if (resp.getBotMessageChainList().isEmpty()) return Collections.emptyList();
		return Collections.singletonList(resp);
	}

	public BotMessage handleEvent(BotRobot bot, BotMessage botMessage) throws Exception {
		return null;
	}


}
