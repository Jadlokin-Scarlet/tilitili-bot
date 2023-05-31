package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseMessageHandleAdapt implements BaseMessageHandle {
	private final List<Long> mockSenderIdList = Arrays.asList(4558L, 4557L, 4556L, 4555L, 4554L, 3752L);

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

	@Override
	public List<BotMessage> mockMessage(BotMessageAction messageAction) {
//		if (mockSenderIdList.contains(messageAction.getBotSender().getId())) {
//			return mockMessageInWaiteSender(messageAction);
//		}
		return null;
	}

	protected List<BotMessage> mockMessageInWaiteSender(BotMessageAction messageAction) {
		return null;
	}
}
