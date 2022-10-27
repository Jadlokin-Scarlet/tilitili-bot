package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.fish.FishGame;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.FishPlayer;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.mapper.mysql.FishPlayerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
	public static final Integer STATUS_WAIT = 0;
	public static final Integer STATUS_FISHING = 1;
	public static final Integer STATUS_COLLECT = 2;
	private final FishGame fishGame;
	private final FishPlayerMapper fishPlayerMapper;

	@Autowired
	public PlayFishGameHandle(FishGame fishGame, FishPlayerMapper fishPlayerMapper) {
		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKey()) {
			case "抛竿": handleStart(messageAction);
			default: throw new AssertException();
		}
//		fishGame.addOperate(messageAction);
	}

	private void handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUser botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getFishPlayerByUserId(userId);
		if (fishPlayer == null) {
			fishPlayer = new FishPlayer();
			fishPlayer.setSenderId(senderId);
			fishPlayer.setUserId(userId);
			fishPlayer.setStatus(STATUS_WAIT);
			fishPlayerMapper.addFishPlayerSelective(fishPlayer);
		}


	}
}
