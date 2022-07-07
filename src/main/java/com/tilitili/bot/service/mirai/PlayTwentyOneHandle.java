package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.TwentyOneTable;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PlayTwentyOneHandle extends ExceptionRespMessageToSenderHandle {
	private final Map<Long, TwentyOneTable> tableMap = new HashMap<>();
	public static final Map<Long, Long> playerLock = new HashMap<>();

	private final BotUserMapper botUserMapper;

	@Autowired
	public PlayTwentyOneHandle(BotUserMapper botUserMapper) {
		this.botUserMapper = botUserMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		Long playerId = messageAction.getQqOrTinyId();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);

		if (!this.checkKeyValid(key, twentyOneTable, playerId)) {
			return null;
		}

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		switch (key) {
			case "玩21点": case "w21": return startGame(messageAction);
			case "加入21点": case "jr21": return prepareGame(messageAction);
			case "加牌": case "jp": return addCard(botUser, twentyOneTable);
			case "停牌": case "tp": return stopCard(botUser, twentyOneTable);
			case "加倍": case "jb": return doubleAddCard(botUser, twentyOneTable);
			default: return null;
		}
	}

	public boolean checkKeyValid(String key, TwentyOneTable twentyOneTable, Long playerId) {
		// 是开始指令则有效
		boolean isStartGame = Arrays.asList("玩21点", "w21").contains(key);
		if (isStartGame) {
			return true;
		}

		// 不是开始指令，游戏还没开始，无效
		boolean notGaming = twentyOneTable == null;
		if (notGaming) {
			return false;
		}

		// 不是开始指令，游戏已经开始了，是当前轮到的玩家，有效
		TwentyOnePlayer lastPlayer = twentyOneTable.getLastPlayer();
		if (lastPlayer == null) {
			return false;
		}
		boolean isLastPlayer = Objects.equals(lastPlayer.getPlayerId(), playerId);
		if (isLastPlayer) {
			return true;
		}

		return false;
	}

	private BotMessage doubleAddCard(BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean doubleAddCardSuccess = twentyOneTable.doubleAddCard(botUser);
		Asserts.isTrue(doubleAddCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage();

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage stopCard(BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean stopCardSuccess = twentyOneTable.stopCard(botUser);
		Asserts.isTrue(stopCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage();

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage addCard(BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean addCardSuccess = twentyOneTable.addCard(botUser);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage();

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserMapper);
			tableMap.put(tableId, twentyOneTable);
			twentyOneTable.startGame();
			return BotMessage.simpleTextMessage("21点准备完毕，请提交入场积分(格式：加入21点 10)");
		} else {
			return BotMessage.simpleTextMessage("21点准备完毕，请提交入场积分(格式：加入21点 10)");
		}
	}

	private BotMessage prepareGame(BotMessageAction messageAction) {
		Long playerId = messageAction.getQqOrTinyId();
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		String scoreStr = messageAction.getValue();

		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) return null;

		if (playerLock.containsKey(playerId)) {
			if (Objects.equals(playerLock.get(playerId), tableId)) {
				return BotMessage.simpleTextMessage("你已经参与啦！").setQuote(messageAction.getMessageId());
			} else {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！").setQuote(messageAction.getMessageId());
			}
		}

		Asserts.isNumber(scoreStr, "格式错啦(积分数)");
		int score = Integer.parseInt(scoreStr);

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		if (score > botUser.getScore()) return BotMessage.simpleTextMessage("积分好像不够惹。").setQuote(messageAction.getMessageId());

		twentyOneTable.addGame(botUser, score);
		botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() - score));
		playerLock.put(playerId, tableId);

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，发牌失败了。");
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage();
			return BotMessage.simpleListMessage(resp);
		} else {
			switch (twentyOneTable.getStatus()) {
				case TwentyOneTable.STATUS_WAIT: return BotMessage.simpleTextMessage("入场成功！请等待他人入场吧").setQuote(messageAction.getMessageId());
				case TwentyOneTable.STATUS_PLAYING: return BotMessage.simpleTextMessage("入场成功！请等待他人入场吧").setQuote(messageAction.getMessageId());
			}
			return null;
		}
	}

}
