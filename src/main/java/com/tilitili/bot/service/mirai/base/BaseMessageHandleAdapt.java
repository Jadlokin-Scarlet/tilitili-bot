package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;

import java.util.Collections;
import java.util.List;

public abstract class BaseMessageHandleAdapt implements BaseMessageHandle {

	@Override
	public List<BotMessage> handleMessageNew(BotMessageAction messageAction) throws Exception {
		BotMessage resp = this.handleMessage(messageAction);
		if (resp == null) return null;
		if (resp.getBotMessageChainList().isEmpty()) return Collections.emptyList();
		return Collections.singletonList(resp);
	}

	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		return null;
	}

	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		return null;
	}

	@Override
	public String getHelpMessage(BotTask botTask) {
		return botTask.getDescription();
	}

	@Override
	public String getHelpMessage(BotTask botTask, String key) {
		return botTask.getDescription();
	}
}
