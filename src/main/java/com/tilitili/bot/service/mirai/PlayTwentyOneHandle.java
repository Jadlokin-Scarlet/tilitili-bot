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
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;

@Slf4j
@Component
public class PlayTwentyOneHandle extends ExceptionRespMessageToSenderHandle {
	private final Map<Long, TwentyOneTable> tableMap = new HashMap<>();

	@Value("${mirai.master-qq}")
	private Long MASTER_QQ;
	@Value("${mirai.master-guild-qq}")
	private Long MASTER_GUILD_QQ;
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
			case "玩21点": case "w21": return this.startGame(messageAction);
			case "准备": case "zb": return this.prepareGame(messageAction);
			case "加牌": case "jp": return this.addCard(messageAction, botUser, twentyOneTable);
			case "停牌": case "tp": return this.stopCard(messageAction, botUser, twentyOneTable);
			case "加倍": case "jb": return this.doubleAddCard(messageAction, botUser, twentyOneTable);
			case "退出": return this.quitGame(messageAction, botUser);
			case "掀桌": return this.removeGame(messageAction, botUser, twentyOneTable);
			default: return null;
		}
	}

	private BotMessage removeGame(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean hasJurisdiction = Objects.equals(messageAction.getBotMessage().getQq(), MASTER_QQ) || Objects.equals(messageAction.getBotMessage().getTinyId(), MASTER_GUILD_QQ);
		if (!hasJurisdiction) return null;
		if (twentyOneTable == null) return null;
		tableMap.remove(twentyOneTable.getTableId());
		return BotMessage.simpleTextMessage("(╯‵□′)╯︵┻━┻");
	}

	private BotMessage quitGame(BotMessageAction messageAction, BotUser botUser) {
		Long playerId = botUser.getExternalId();
		TwentyOneTable twentyOneTable = this.getTableByPlayer(playerId);
		Asserts.notNull(twentyOneTable, "你好像还没加入哦，没东西退");

		TwentyOnePlayer player = twentyOneTable.getPlayer(playerId);
		String status = twentyOneTable.getStatus();

		boolean removePlayerSuccess = twentyOneTable.removePlayer(playerId);
		Asserts.isTrue(removePlayerSuccess, "啊嘞，不太对劲");

		if (Objects.equals(status, TwentyOneTable.STATUS_PLAYING)) {
			return BotMessage.simpleTextMessage("退出成功啦，游戏已经开始，不返还积分。");
		}

		List<BotMessageChain> resp = new ArrayList<>();

		if (Objects.equals(status, TwentyOneTable.STATUS_WAIT) && player.getScore() != null) {
			botUserMapper.updateBotUserSelective(new BotUser().setId(player.getPlayerId()).setScore(botUser.getScore() + player.getScore()));
			resp.add(BotMessageChain.ofPlain(String.format("退出成功啦。返还积分%d，剩余%d积分。", player.getScore(), botUser.getScore() + player.getScore())));
		} else {
			resp.add(BotMessageChain.ofPlain("退出成功啦。"));
		}

		if (twentyOneTable.getPlayerList().isEmpty()) {
			return BotMessage.simpleListMessage(resp);
		} else {
			resp.add(BotMessageChain.ofPlain("\n"));
		}

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，发牌失败了。");
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
		boolean isStartGame = Arrays.asList("玩21点", "w21", "准备", "zb").contains(key);
		if (isStartGame) {
			return true;
		}

		boolean notGaming = twentyOneTable == null;
		if (notGaming) {
			return false;
		}

		boolean isEndGame = Arrays.asList("掀桌", "退出").contains(key);
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

	private BotMessage doubleAddCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		Integer hasScore = botUser.getScore();
		TwentyOnePlayer player = twentyOneTable.getPlayer(botUser.getExternalId());
		Asserts.notNull(player, "啊嘞，有点不对劲");
		Asserts.isTrue(player.getCardList().size() == 2, "已经不能双倍了哦");
		Integer useScore = player.getScore();
		Asserts.isTrue(useScore <= hasScore, "积分好像不够惹。");
		player.setScore(useScore * 2);
		botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(hasScore - useScore));

		boolean addCardSuccess = twentyOneTable.addCard(player);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		boolean stopCardSuccess = twentyOneTable.stopCard(player);
		Asserts.isTrue(stopCardSuccess, "啊嘞，不对劲");

//		botManager.sendMessage(BotMessage.simpleTextMessage(String.format("加倍完毕，当前积分总和%d，剩余%d积分。", useScore * 2, hasScore - useScore), messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));

		if (twentyOneTable.isEnd()) {
			List<BotMessageChain> resp = twentyOneTable.getEndMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		} else {
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		}
	}

	private BotMessage stopCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
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

	private BotMessage addCard(BotMessageAction messageAction, BotUser botUser, TwentyOneTable twentyOneTable) {
		boolean addCardSuccess = twentyOneTable.addCard(botUser);
		Asserts.isTrue(addCardSuccess, "啊嘞，不对劲");

		List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());

		return BotMessage.simpleListMessage(resp);
	}

	private BotMessage startGame(BotMessageAction messageAction) {
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		Long playerId = messageAction.getQqOrTinyId();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);
		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserMapper, botManager, messageAction);
			tableMap.put(tableId, twentyOneTable);
		}
		Asserts.isTrue(twentyOneTable.getPlayerList().size() < 4, "人数爆满啦，稍后再来吧。");

		TwentyOneTable otherTable = this.getTableByPlayer(playerId);
		if (otherTable != null) {
			if (Objects.equals(otherTable.getTableId(), tableId)) {
				return BotMessage.simpleTextMessage("你已经参与啦！").setQuote(messageAction.getMessageId());
			} else {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！").setQuote(messageAction.getMessageId());
			}
		}

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		Asserts.isTrue(botUser.getScore() > 0, "你没有积分啦！");
		boolean addGameSuccess = twentyOneTable.addGame(botUser);
		Asserts.isTrue(addGameSuccess, "加入失败惹。");

		switch (twentyOneTable.getStatus()) {
			case TwentyOneTable.STATUS_WAIT: return BotMessage.simpleTextMessage("入场成功！请尽快提交入场积分。格式：(准备 10)").setQuote(messageAction.getMessageId());
			case TwentyOneTable.STATUS_PLAYING: return BotMessage.simpleTextMessage("入场成功！请等待下一局吧。").setQuote(messageAction.getMessageId());
			default: throw new AssertException("啊嘞，似乎不对劲");
		}
	}

	private BotMessage prepareGame(BotMessageAction messageAction) {
		Long playerId = messageAction.getQqOrTinyId();
		Long tableId = messageAction.getQqOrGroupOrChannelId();
		String scoreStr = messageAction.getValue();
		TwentyOneTable twentyOneTable = tableMap.get(tableId);

		if (StringUtils.isNotDigits(scoreStr)) {
			log.info("格式错啦(积分数)");
			return null;
		}

		if (twentyOneTable == null) {
			twentyOneTable = new TwentyOneTable(botUserMapper, botManager, messageAction);
			tableMap.put(tableId, twentyOneTable);
		}
		Asserts.isTrue(twentyOneTable.getPlayerList().size() < 4, "人数爆满啦，稍后再来吧。");

		TwentyOnePlayer player = twentyOneTable.getPlayer(playerId);
		TwentyOneTable otherTable = this.getTableByPlayer(playerId);
		if (otherTable != null) {
			if (player != null && player.getScore() != null) {
				return BotMessage.simpleTextMessage("你已经参与啦！").setQuote(messageAction.getMessageId());
			} else if (!Objects.equals(otherTable.getTableId(), tableId)) {
				return BotMessage.simpleTextMessage("你已经在别的地方参与啦！").setQuote(messageAction.getMessageId());
			}
		}
		Asserts.checkEquals(twentyOneTable.getStatus(), TwentyOneTable.STATUS_WAIT, "游戏进行中哦，请稍等。");

		int score = Integer.parseInt(scoreStr);
		Asserts.isTrue(score > 0, "想白嫖积分？");

		BotUser botUser = botUserMapper.getBotUserByExternalId(playerId);
		if (score > botUser.getScore()) return BotMessage.simpleTextMessage("积分好像不够惹。").setQuote(messageAction.getMessageId());

		boolean addGameSuccess = twentyOneTable.addGame(botUser, score);
		Asserts.isTrue(addGameSuccess, "啊嘞，不对劲");

		botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() - score));

//		botManager.sendMessage(BotMessage.simpleTextMessage(String.format("准备完毕，使用积分%d，剩余%d积分。", score, botUser.getScore() - score), messageAction.getBotMessage()).setQuote(messageAction.getMessageId()));

		boolean ready = twentyOneTable.isReady();
		if (ready) {
			boolean flashCardSuccess = twentyOneTable.flashCard();
			Asserts.isTrue(flashCardSuccess, "啊嘞，发牌失败了。");
			List<BotMessageChain> resp = twentyOneTable.getNoticeMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		} else {
			List<BotMessageChain> resp = twentyOneTable.getWaitMessage(messageAction.getBotMessage());
			return BotMessage.simpleListMessage(resp);
		}
	}
}
