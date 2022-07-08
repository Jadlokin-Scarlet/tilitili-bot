package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.TwentyOneTable;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
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
	private final BotManager botManager;

	@Autowired
	public PlayTwentyOneHandle(BotUserMapper botUserMapper, BotManager botManager) {
		this.botUserMapper = botUserMapper;
		this.botManager = botManager;
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
			case "加入21点": case "准备": return prepareGame(messageAction);
			case "加牌": case "jp": return addCard(messageAction, botUser, twentyOneTable);
			case "停牌": case "tp": return stopCard(messageAction, botUser, twentyOneTable);
			case "加倍": case "jb": return doubleAddCard(messageAction, botUser, twentyOneTable);
			case "退出": return quitGame(messageAction, botUser);
			default: return null;
		}
	}

	private BotMessage quitGame(BotMessageAction messageAction, BotUser botUser) {
		Long playerId = botUser.getExternalId();
		Long tableId = playerLock.get(playerId);
		Asserts.notNull(tableId, "你好像还没加入哦，没东西退");
		playerLock.remove(playerId);

		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		TwentyOnePlayer player = twentyOneTable.getPlayer(playerId);
		String status = twentyOneTable.getStatus();

		boolean removePlayerSuccess = twentyOneTable.removePlayer(playerId);
		Asserts.isTrue(removePlayerSuccess, "啊嘞，不太对劲");

		if (Objects.equals(status, TwentyOneTable.STATUS_WAIT) && player.getScore() != null) {
			botUserMapper.updateBotUserSelective(new BotUser().setId(player.getPlayerId()).setScore(botUser.getScore() + player.getScore()));
			return BotMessage.simpleTextMessage(String.format("退出成功啦。返还积分%d，剩余%d积分", player.getScore(), botUser.getScore() + player.getScore()));
		} else if (Objects.equals(status, TwentyOneTable.STATUS_PLAYING)) {
			return BotMessage.simpleTextMessage("退出成功啦，游戏已经开始，不返还积分。");
		} else {
			return BotMessage.simpleTextMessage("退出成功啦。");
		}
	}

	public boolean checkKeyValid(String key, TwentyOneTable twentyOneTable, Long playerId) {
		// 是开始指令则有效
		boolean isStartGame = Arrays.asList("玩21点", "w21").contains(key);
		if (isStartGame) {
			return true;
		}

		// 不是开始指令，游戏还不存在，无效
		boolean notGaming = twentyOneTable == null;
		if (notGaming) {
			return false;
		}

		// 游戏已经存在了，是准备指令，有效
		boolean isJoinGame = Arrays.asList("加入21点", "准备", "退出").contains(key);
		if (isJoinGame) {
			return true;
		}

		// 不是准备和开始指令，游戏还没开始，，
		String status = twentyOneTable.getStatus();
		if (status == TwentyOneTable.STATUS_WAIT) {
			return false;
		}


		// 不是准备和开始指令，游戏已经开始了，是当前轮到的玩家，有效
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

	private BotMessage doubleAddCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean doubleAddCardSuccess = twentyOneTable.doubleAddCard(botUser);
		Asserts.isTrue(doubleAddCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage().getSendType());

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage stopCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean stopCardSuccess = twentyOneTable.stopCard(botUser);
		Asserts.isTrue(stopCardSuccess, "啊嘞，不对劲");
		if (twentyOneTable.isEnd()) {
			List<BotMessageChain> resp = twentyOneTable.getEndMessage();
			return BotMessage.simpleListMessage(resp);
		} else {
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage().getSendType());
			return BotMessage.simpleListMessage(resp);
		}
	}

	private BotMessage addCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean addCardSuccess = twentyOneTable.addCard(botUser);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage().getSendType());

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		Long playerId = messageAction.getQqOrTinyId();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserMapper, tableId);
			tableMap.put(tableId, twentyOneTable);
		}

		if (playerLock.containsKey(playerId)) {
			if (Objects.equals(playerLock.get(playerId), tableId)) {
				return BotMessage.simpleTextMessage("你已经参与啦！").setQuote(messageAction.getMessageId());
			} else {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！").setQuote(messageAction.getMessageId());
			}
		}

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		boolean addGameSuccess = twentyOneTable.addGame(botUser);
		Asserts.isTrue(addGameSuccess, "加入失败惹。");
		playerLock.put(playerId, tableId);

		switch (twentyOneTable.getStatus()) {
			case TwentyOneTable.STATUS_WAIT: return BotMessage.simpleTextMessage("入场成功！请提交入场积分。格式：(准备 10)").setQuote(messageAction.getMessageId());
			case TwentyOneTable.STATUS_PLAYING: return BotMessage.simpleTextMessage("入场成功！请等待下一局吧。").setQuote(messageAction.getMessageId());
			default: throw new AssertException("啊嘞，似乎不对劲");
		}
	}

	private BotMessage prepareGame(BotMessageAction messageAction) {
		Long playerId = messageAction.getQqOrTinyId();
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		String scoreStr = messageAction.getValue();

		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) return null;

		Asserts.isNumber(scoreStr, "格式错啦(积分数)");
		int score = Integer.parseInt(scoreStr);
		Asserts.isTrue(score > 0, "想白嫖积分？");

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		if (score > botUser.getScore()) return BotMessage.simpleTextMessage("积分好像不够惹。").setQuote(messageAction.getMessageId());

		twentyOneTable.addGame(botUser, score);
		botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() - score));

		botManager.sendMessage(BotMessage.simpleTextMessage(String.format("准备完毕，使用积分%d，剩余%d积分。", score, botUser.getScore() - score), messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，发牌失败了。");
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage().getSendType());
			return BotMessage.simpleListMessage(resp);
		} else {
			return BotMessage.emptyMessage();
		}
	}

}
