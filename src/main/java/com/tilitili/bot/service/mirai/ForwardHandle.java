package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import org.springframework.stereotype.Component;

@Component
public class ForwardHandle extends ExceptionRespMessageHandle {
	private final BotSender baGroup;

	public ForwardHandle(BotSenderMapper botSenderMapper) {
		this.baGroup = botSenderMapper.getBotSenderById(3759L);
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		if (messageAction.getBotSender().getId() == 4370L) {
			return BotMessage.simpleListMessage(messageAction.getBotMessage().getBotMessageChainList()).setSender(baGroup);
		}
		return null;
	}
}
