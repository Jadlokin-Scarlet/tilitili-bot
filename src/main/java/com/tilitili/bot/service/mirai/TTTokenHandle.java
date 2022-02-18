package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.TwitterAuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TTTokenHandle extends ExceptionRespMessageHandle {
	private final TwitterAuthManager twitterAuthManager;

	@Autowired
	public TTTokenHandle(TwitterAuthManager twitterAuthManager) {
		this.twitterAuthManager = twitterAuthManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String token = twitterAuthManager.getToken();
		return BotMessage.simpleTextMessage(token, messageAction.getBotMessage());
	}
}
