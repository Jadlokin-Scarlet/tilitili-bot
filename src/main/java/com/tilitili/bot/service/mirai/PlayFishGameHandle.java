package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.fish.FishGame;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
	private final FishGame fishGame;

	@Autowired
	public PlayFishGameHandle(FishGame fishGame) {
		this.fishGame = fishGame;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		fishGame.addOperate(messageAction);
		return BotMessage.emptyMessage();
	}
}
