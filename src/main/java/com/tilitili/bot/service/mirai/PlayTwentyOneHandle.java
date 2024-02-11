package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.TwentyOneTable;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.entity.twentyOne.TwentyOneCardList;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotRoleManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;

@Slf4j
@Component
public class PlayTwentyOneHandle extends ExceptionRespMessageToSenderHandle {
	private final Map<Long, TwentyOneTable> tableMap = new HashMap<>();

	private final BotUserManager botUserManager;
	private final SendMessageManager sendMessageManager;
	private final BotRoleManager botRoleManager;

	@Autowired
	public PlayTwentyOneHandle(BotUserManager botUserManager, SendMessageManager sendMessageManager, BotRoleManager botRoleManager) {
		this.botUserManager = botUserManager;
		this.sendMessageManager = sendMessageManager;
		this.botRoleManager = botRoleManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String key = messageAction.getKeyWithoutPrefix();
		Long tableId = messageAction.getBotSender().getId();
		BotUserDTO botUser = messageAction.getBotUser();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		Long playerId = botUser.getId();

		if (!this.checkKeyValid(key, twentyOneTable, playerId)) {
			return null;
		}
		if (twentyOneTable != null) {
			log.info(Gsons.toJson(twentyOneTable.getPlayerList()));
		}

//		Asserts.notNull(botUser.getScore(), "未绑定");

		switch (key) {
			case "买水果小游戏": return this.startGame(messageAction);
			case "准备": case "zb": return this.prepareGame(messageAction);
			case "进货": case "jh": return this.addCard(messageAction, botUser, twentyOneTable);
			case "摆烂": case "bl": return this.stopCard(messageAction, botUser, twentyOneTable);
			case "孤注一掷": case "gzyz": return this.doubleAddCard(messageAction, botUser, twentyOneTable);
			case "分家": case "fj": return this.splitCard(messageAction, twentyOneTable);
			case "投降": case "tx": return this.surrender(messageAction, twentyOneTable);
			case "退出": return this.quitGame(messageAction, botUser);
			case "掀桌": return this.removeGame(messageAction, twentyOneTable);
			case "桌面": return this.showTable(messageAction, twentyOneTable);
			default: return null;
		}
	}

	private BotMessage showTable(BotMessageAction messageAction, TwentyOneTable twentyOneTable) {
		return BotMessage.simpleListMessage(twentyOneTable.getNoticeMessage(messageAction.getBotMessage()));
	}

	private BotMessage surrender(BotMessageAction messageAction, TwentyOneTable twentyOneTable) {
		BotUserDTO botUser = messageAction.getBotUser();
		TwentyOnePlayer player = twentyOneTable.getPlayerByPlayerId(botUser.getId());
		Asserts.notNull(player, "啊嘞，有点不对劲");
		Asserts.isTrue(player.isPrepare(), "你还没准备呢。");
		Asserts.checkEquals(player.getCardListList().size(), 1, "现在不能投降了哦。");
		Asserts.checkEquals(player.getFirstNoEndCardList().getCardList().size(), 2, "现在不能投降了哦。");

		int score = 0;
		for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
			score += twentyOneCardList.getScore();
		}
		score /= 2;

		List<BotMessageChain> resp = new ArrayList<>();
		botUserManager.safeUpdateScore(botUser, score);
		resp.add(BotMessageChain.ofPlain(String.format("返还%s积分%d，剩余%d积分。\n", botUser.getName(), score, botUser.getScore() + score)));

		twentyOneTable.initPlayer(player);
		if (twentyOneTable.isEnd()) {
			resp.addAll(twentyOneTable.getEndMessage(messageAction.getBotMessage()));
		} else {
			resp.addAll(twentyOneTable.getNoticeMessage(messageAction.getBotMessage()));
		}

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage splitCard(BotMessageAction messageAction, TwentyOneTable twentyOneTable) {
		BotUserDTO botUser = messageAction.getBotUser();
		Integer hasScore = botUser.getScore();
		TwentyOnePlayer player = twentyOneTable.getPlayerByPlayerId(botUser.getId());
		Asserts.notNull(player, "啊嘞，有点不对劲");
		TwentyOneCardList twentyOneCardList = player.getFirstNoEndCardList();
		Asserts.isTrue(twentyOneCardList.getCardList().size() == 2, "已经不能分家了哦");
		Integer useScore = twentyOneCardList.getScore();
		Asserts.isTrue(useScore <= hasScore, "积分好像不够惹。");
		boolean splitCardSuccess = twentyOneTable.splitCard(player);
		Asserts.isTrue(splitCardSuccess, "啊嘞，不对劲");

		botUserManager.safeUpdateScore(botUser, - useScore);

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());
		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage removeGame(BotMessageAction messageAction, TwentyOneTable twentyOneTable) {
		boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
		if (!canUseBotAdminTask) return null;
		if (twentyOneTable == null) return null;
		tableMap.remove(twentyOneTable.getTableId());
		return BotMessage.simpleTextMessage("(╯‵□′)╯︵┻━┻");
	}

	private BotMessage quitGame(BotMessageAction messageAction, BotUserDTO botUser) {
		Long playerId = botUser.getId();
		TwentyOneTable twentyOneTable = this.getTableByPlayer(playerId);
		Asserts.notNull(twentyOneTable, "你好像还没加入哦，没东西退");

		TwentyOnePlayer player = twentyOneTable.getPlayerByPlayerId(playerId);
		String status = twentyOneTable.getStatus();

		boolean removePlayerSuccess = twentyOneTable.removePlayer(playerId);
		Asserts.isTrue(removePlayerSuccess, "啊嘞，不太对劲");

		if (Objects.equals(status, TwentyOneTable.STATUS_PLAYING)) {
			return BotMessage.simpleTextMessage("退出成功啦，游戏已经开始，不返还积分。");
		}

		List<BotMessageChain> resp = new ArrayList<>();

		if (Objects.equals(status, TwentyOneTable.STATUS_WAIT) && player.isPrepare()) {
			int sum = 0;
			for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
				sum += twentyOneCardList.getScore();
			}
			botUserManager.safeUpdateScore(botUser, sum);
			resp.add(BotMessageChain.ofPlain(String.format("退出成功啦。返还积分%d，剩余%d积分。", sum, botUser.getScore() + sum)));
		} else {
			resp.add(BotMessageChain.ofPlain("退出成功啦。"));
		}

		if (twentyOneTable.getPlayerList().isEmpty()) {
			twentyOneTable.initData();
			return BotMessage.simpleListMessage(resp);
		} else {
			resp.add(BotMessageChain.ofPlain("\n"));
		}

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，不对劲。");
			resp.addAll(twentyOneTable.getNoticeMessage(messageAction.getBotMessage()));
			return BotMessage.simpleListMessage(resp);
		} else {
			resp.addAll(twentyOneTable.getWaitMessage(messageAction.getBotMessage()));
			return BotMessage.simpleListMessage(resp);
		}
	}

	private TwentyOneTable getTableByPlayer(Long playerId) {
		return tableMap.values().stream().filter(table ->
					table.getPlayerList().stream().map(TwentyOnePlayer::getPlayerId).anyMatch(Predicate.isEqual(playerId))
			).findFirst().orElse(null);
	}

	public boolean checkKeyValid(String key, TwentyOneTable twentyOneTable, Long playerId) {
		// 是开始指令则有效
		boolean isStartGame = Arrays.asList("买水果小游戏", "准备", "zb").contains(key);
		if (isStartGame) {
			return true;
		}

		boolean notGaming = twentyOneTable == null;
		if (notGaming) {
			return false;
		}

		boolean isEndGame = Arrays.asList("掀桌", "退出", "桌面").contains(key);
		if (isEndGame) {
			return true;
		}


		// 不是准备和开始指令，游戏还没开始，，
		String status = twentyOneTable.getStatus();
		if (Objects.equals(status, TwentyOneTable.STATUS_WAIT)) {
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

	private BotMessage doubleAddCard(BotMessageAction messageAction, BotUserDTO botUser, TwentyOneTable twentyOneTable) {
		Integer hasScore = botUser.getScore();
		TwentyOnePlayer player = twentyOneTable.getPlayerByPlayerId(botUser.getId());
		Asserts.notNull(player, "啊嘞，有点不对劲");
		TwentyOneCardList twentyOneCardList = player.getFirstNoEndCardList();
		Asserts.isTrue(twentyOneCardList.getCardList().size() == 2, "已经不能孤注一掷了哦");
		Integer useScore = twentyOneCardList.getScore();
		Asserts.isTrue(useScore <= hasScore, "积分好像不够惹。");
		twentyOneCardList.setScore(useScore * 2);
		botUserManager.safeUpdateScore(botUser, - useScore);

		boolean addCardSuccess = twentyOneTable.addCard(player);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		boolean stopCardSuccess = twentyOneTable.stopCard(player);
		Asserts.isTrue(stopCardSuccess, "啊嘞，不对劲");

//		botManager.sendMessage(BotMessage.simpleTextMessage(String.format("加倍完毕，当前积分总和%d，剩余%d积分。", useScore * 2, hasScore - useScore), messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));

		List<BotMessageChain> resp;
		if (twentyOneTable.isEnd()) {
			resp = twentyOneTable.getEndMessage(messageAction.getBotMessage());
		} else {
			resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());
		}
		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage stopCard(BotMessageAction messageAction, BotUserDTO botUser, TwentyOneTable twentyOneTable) {
		boolean stopCardSuccess = twentyOneTable.stopCard(botUser);
		Asserts.isTrue(stopCardSuccess, "啊嘞，不对劲");
		if (twentyOneTable.isEnd()) {
			List<BotMessageChain> resp = twentyOneTable.getEndMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		} else {
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		}
	}

	private BotMessage addCard(BotMessageAction messageAction, BotUserDTO botUser, TwentyOneTable twentyOneTable) {
		boolean addCardSuccess = twentyOneTable.addCard(botUser);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		Long tableId = messageAction.getBotSender().getId();
		BotUserDTO botUser = messageAction.getBotUser();
		Long playerId = botUser.getId();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserManager, sendMessageManager, messageAction);
			tableMap.put(tableId, twentyOneTable);
		}
		Asserts.isTrue(twentyOneTable.getPlayerList().size() < 1, "人数爆满啦，稍后再来吧。");

		TwentyOneTable otherTable = this.getTableByPlayer(playerId);
		if (otherTable != null) {
			if (Objects.equals(otherTable.getTableId(), tableId)) {
				return BotMessage.simpleTextMessage("你已经参与啦！", botMessage).setQuote(messageAction.getMessageId());
			} else {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！", botMessage).setQuote(messageAction.getMessageId());
			}
		}

		Asserts.isTrue(botUser.getScore() > 0, "你没有积分啦！");
		boolean addGameSuccess = twentyOneTable.addGame(botUser);
		Asserts.isTrue(addGameSuccess, "加入失败惹。");

		switch (twentyOneTable.getStatus()) {
			case TwentyOneTable.STATUS_WAIT:
				twentyOneTable.waitPeoplePrepare(botMessage);
				return BotMessage.simpleTextMessage("加入成功！请尽快准备吧。格式：(准备 10)", botMessage).setQuote(messageAction.getMessageId());
			case TwentyOneTable.STATUS_PLAYING: return BotMessage.simpleTextMessage("加入成功！请等待下一回合吧。", botMessage).setQuote(messageAction.getMessageId());
			default: throw new AssertException("啊嘞，似乎不对劲");
		}
	}

	private BotMessage prepareGame(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		BotUserDTO botUser = messageAction.getBotUser();
		Long playerId = botUser.getId();
		Long tableId = messageAction.getBotSender().getId();
		String scoreStr = messageAction.getValue();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);

		if (StringUtils.isNotDigits(scoreStr)) {
			log.info("格式错啦(积分数)");
			return null;
		}

		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserManager, sendMessageManager, messageAction);
			tableMap.put(tableId, twentyOneTable);
		}

		TwentyOnePlayer player = twentyOneTable.getPlayerByPlayerId(playerId);
		TwentyOneTable otherTable = this.getTableByPlayer(playerId);
		if (otherTable != null) {
			if (player != null && player.isPrepare()) {
				return BotMessage.simpleTextMessage("你已经参与啦！", botMessage).setQuote(messageAction.getMessageId());
			} else if (!Objects.equals(otherTable.getTableId(), tableId)) {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！", botMessage).setQuote(messageAction.getMessageId());
			}
		}
		Asserts.checkEquals(twentyOneTable.getStatus(), TwentyOneTable.STATUS_WAIT, "游戏进行中哦，请稍等。");

		int score = Integer.parseInt(scoreStr);
		Asserts.isTrue(score > 0, "想白嫖积分？");
		if (score > botUser.getScore()) {
			return BotMessage.simpleTextMessage("积分好像不够惹。", botMessage).setQuote(messageAction.getMessageId());
		}

		boolean addGameSuccess = twentyOneTable.addGame(botUser, score);
		Asserts.isTrue(addGameSuccess, "啊嘞，不对劲");

		botUserManager.safeUpdateScore(botUser, - score);

//		botManager.sendMessage(BotMessage.simpleTextMessage(String.format("准备完毕，使用积分%d，剩余%d积分。", score, botUser.getScore() - score), messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，不对劲。");
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(botMessage);
			return BotMessage.simpleListMessage(resp);
		} else {
			List<BotMessageChain> resp = twentyOneTable.getWaitMessage(botMessage);
			return BotMessage.simpleListMessage(resp);
		}
	}
}
