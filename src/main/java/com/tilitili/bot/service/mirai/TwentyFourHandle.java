package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.stereotype.Component;

//@Component
public class TwentyFourHandle extends ExceptionRespMessageHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		return null;
	}
}
