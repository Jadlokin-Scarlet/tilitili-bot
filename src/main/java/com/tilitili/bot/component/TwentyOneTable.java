package com.tilitili.bot.component;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.entity.twentyOne.*;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.SortObject;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TwentyOneTable {
	public static final String STATUS_WAIT = "wait";
	public static final String STATUS_PLAYING = "playing";
	public static final String FIVE_CARD = "峰回路转";
	public static final String BLACK_JACK = "天选之子";
	public static final String BOOM_CARD = "时运不济";
	private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
	private final static Random random = new Random(System.currentTimeMillis());
	private final BotUserManager botUserManager;
	private final SendMessageManager sendMessageManager;
	private final Long tableId;
	private String status;
	private List<TwentyOnePlayer> playerList;
	private Queue<TwentyOneCard> cardList;
	private final TwentyOneAdmin admin;
	private Long waitPeoplePrepareId;

	public TwentyOneTable(BotUserManager botUserManager, SendMessageManager sendMessageManager, BotMessageAction messageAction) {
		this.botUserManager = botUserManager;
		this.sendMessageManager = sendMessageManager;
		this.tableId = messageAction.getBotSender().getId();
		this.status = STATUS_WAIT;
		this.playerList = new ArrayList<>();
		this.cardList = this.newCardList();
		this.admin = new TwentyOneAdmin();
		this.waitPeoplePrepareId = null;
		this.waitPeoplePrepare(messageAction.getBotMessage());
	}

	public void waitPeoplePrepare(BotMessage botMessage) {
		if (playerList.isEmpty()) {
			this.initData();
			return;
		}
		if (waitPeoplePrepareId != null) return;
		final long theWaitPeoplePrepareId = random.nextLong();
		this.waitPeoplePrepareId = theWaitPeoplePrepareId;
		scheduled.schedule(() -> {
			if (theWaitPeoplePrepareId != this.waitPeoplePrepareId) {
				return;
			}
			if (Objects.equals(this.getStatus(), STATUS_PLAYING)) {
				return;
			}
			if (playerList.isEmpty()) {
				return;
			}
			this.playerList = this.getGamingPlayerList();
			if (playerList.isEmpty()) {
				this.initData();
				try {
					sendMessageManager.sendMessage(BotMessage.simpleTextMessage("游戏结束啦", botMessage));
				} catch (Exception e) {
					log.error("21点提示异常",e);
				}
			} else {
				boolean flashCardSuccess = this.flashCard();
				Asserts.isTrue(flashCardSuccess, "啊嘞，不对劲。");
				List<BotMessageChain> resp = this.getNoticeMessage(botMessage);
				try {
					sendMessageManager.sendMessage(BotMessage.simpleListMessage(resp, botMessage));
				} catch (Exception e) {
					log.error("21点提示异常", e);
				}
			}
			this.endWait();
		}, 1, TimeUnit.MINUTES);
	}

	public void endWait() {
		this.waitPeoplePrepareId = null;
	}

	public boolean addGame(BotUserDTO botUser, Integer score) {
		TwentyOnePlayer player = getPlayerByPlayerId(playerList, botUser.getId());
		if (player == null) {
			Asserts.isTrue(playerList.size() < 4, "人数爆满啦，稍后再来吧。");
			playerList.add(new TwentyOnePlayer().setBotUser(botUser).setCardListList(Lists.newArrayList(
					new TwentyOneCardList().setScore(score)
			)));
		} else {
			player.getFirstNoEndCardList().setScore(score);
		}
		return true;
	}

	public boolean addGame(BotUserDTO botUser) {
		TwentyOnePlayer player = getPlayerByPlayerId(playerList, botUser.getId());
		if (player == null) {
			playerList.add(new TwentyOnePlayer().setBotUser(botUser).setCardListList(Lists.newArrayList(
					new TwentyOneCardList()
			)));
			return true;
		} else {
			return false;
		}
	}

	public boolean removePlayer(Long playerId) {
		this.playerList = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId).negate()).collect(Collectors.toList());
		return true;
	}

	public boolean flashCard() {
		TwentyOneCard hiddenCard = cardList.remove();
		Asserts.notNull(hiddenCard, "啊嘞，不对劲。");
		admin.setCardList(Lists.newArrayList(hiddenCard.setHidden(true), cardList.remove()));
		for (TwentyOnePlayer player : this.getGamingPlayerList()) {
			for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
				twentyOneCardList.setCardList(Lists.newArrayList(cardList.remove(), cardList.remove()));
			}
		}
		status = STATUS_PLAYING;
		return true;
	}

	public boolean splitCard(TwentyOnePlayer player) {
		Asserts.checkEquals(player.getCardListList().size(), 1, "你已经分过啦。");
		TwentyOneCardList twentyOneCardList = player.getFirstNoEndCardList();
		List<TwentyOneCard> firstCardList = twentyOneCardList.getCardList();
		TwentyOneCard firstRemoveCard = firstCardList.get(1);
		Asserts.checkEquals(firstCardList.get(0).getValue(), firstRemoveCard.getValue(), "要一样的才能分哦。");
		firstCardList.remove(1);
		firstCardList.add(cardList.remove());
		player.getCardListList().add(new TwentyOneCardList().setScore(twentyOneCardList.getScore()).setCardList(
				Lists.newArrayList(firstRemoveCard, cardList.remove())
		));
		return true;
	}

	public List<BotMessageChain> getWaitMessage(BotMessage botMessage) {
		List<String> allPlayer = new ArrayList<>();
		List<String> preparePlayer = new ArrayList<>();
		List<String> notPreparePlayer = new ArrayList<>();
		for (TwentyOnePlayer player : playerList) {
			for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
				String name = player.getBotUser().getName();
				allPlayer.add(name);
				if (twentyOneCardList.getScore() == null) {
					notPreparePlayer.add(name);
				} else {
					preparePlayer.add(name);
				}
			}
		}
		this.waitPeoplePrepare(botMessage);
		String message = String.format("本回合参与人员：%s\n已准备：%s\n未准备：%s", String.join(",", allPlayer), String.join(",", preparePlayer), String.join(",", notPreparePlayer));
		return Lists.newArrayList(BotMessageChain.ofPlain(message));
	}

	public List<BotMessageChain> getNoticeMessage(BotMessage botMessage) {
		TwentyOnePlayer nowPlayer = this.getLastPlayer();
		CardResult adminCardResult = this.getCardResult(this.admin.getCardList());
		while (nowPlayer != null && nowPlayer.needEnd(this.getCardResult(nowPlayer.getFirstNoEndCardList().getCardList()))) {
			this.stopCard(nowPlayer);
			nowPlayer = this.getLastPlayer();
		}
		if (this.isEnd()) return this.getEndMessage(botMessage);

		if (Objects.equals(adminCardResult.getSuperCard(), BLACK_JACK)) {
			for (TwentyOnePlayer player : playerList) {
				this.stopCard(player);
			}
			if (this.isEnd()) return this.getEndMessage(botMessage);
			nowPlayer = this.getLastPlayer();
		}

		Asserts.notNull(nowPlayer, "啊嘞，似乎不对劲");
		TwentyOneCardList nowTwentyOneCardList = nowPlayer.getFirstNoEndCardList();

		String adminStr = this.admin.toString();
		String nowIndex = "→ ";
		List<BotMessageChain> result = Lists.newArrayList(BotMessageChain.ofPlain(adminStr));
		for (TwentyOnePlayer player : this.getGamingPlayerList()) {
			for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
				String cardListStr = twentyOneCardList.getCardList().stream().map(TwentyOneCard::toString).collect(Collectors.joining(","));
				if (twentyOneCardList.getStatus() == 1) {
					result.add(BotMessageChain.ofPlain(String.format("\n%s：%s", player.getBotUser().getName(), cardListStr)));
				} else {
					result.add(BotMessageChain.ofPlain(String.format("\n%s%s：%s", nowIndex, player.getBotUser().getName(), cardListStr)));
					nowIndex = "";
				}
			}
		}
		result.add(BotMessageChain.ofPlain("\n"));
		if (!Objects.equals(botMessage.getBotSender().getSendType(), SendTypeEnum.FRIEND_MESSAGE_STR)) {
			result.add(BotMessageChain.ofAt(nowPlayer.getBotUser()));
		}
		List<String> chooseList = Lists.newArrayList("进货", "摆烂");
		if (nowPlayer.getCardListList().size() == 1 && nowTwentyOneCardList.getCardList().size() == 2) {
			chooseList.add("投降");
			if (Objects.equals(nowTwentyOneCardList.getCardList().get(0).getValue(), nowTwentyOneCardList.getCardList().get(1).getValue())) {
				chooseList.add("分家");
			}
		}
		if (nowTwentyOneCardList.getCardList().size() == 2) {
			chooseList.add("孤注一掷");
		}
		result.add(BotMessageChain.ofPlain("请选择："+String.join("、", chooseList)));
		this.endWait();
		return result;
	}

	// 结算
	public List<BotMessageChain> getEndMessage(BotMessage botMessage) {
		// 庄家翻牌
		admin.getCardList().get(0).setHidden(false);

		// 抽牌
		CardResult adminCardResult;
		List<TwentyOnePlayer> gamingPlayerList = this.getGamingPlayerList();
		while (this.needAddCard(gamingPlayerList, adminCardResult = this.getCardResult(admin.getCardList()))) {
			admin.addCard(cardList.remove());
		}
		List<BotMessageChain> resp = new ArrayList<>();
		String adminStr = String.format("%s (%s)", admin, adminCardResult);
		resp.add(BotMessageChain.ofPlain(adminStr));
		// 对比
		for (TwentyOnePlayer player : gamingPlayerList) {
			for (TwentyOneCardList twentyOneCardList : player.getCardListList()) {
				CardResult playerResult = this.getCardResult(twentyOneCardList.getCardList());
				int subScore = this.compareCard(adminCardResult, playerResult, twentyOneCardList);
				String cardListStr = twentyOneCardList.getCardList().stream().map(TwentyOneCard::toString).collect(Collectors.joining(","));
				String playerStr = String.format("\n%s：%s (%s) (%s分)", player.getBotUser().getName(), cardListStr, playerResult, subScore > 0? "+" + subScore: "" + subScore);
				resp.add(BotMessageChain.ofPlain(playerStr));
				BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(tableId, player.getBotUser().getId());
				try {
					botUserManager.safeUpdateScore(botUser, subScore + twentyOneCardList.getScore());
				} catch (Exception e) {
					resp.add(BotMessageChain.ofPlain("(积分更新失败)"));
				}
			}
		}
		List<TwentyOnePlayer> newPlayerList = new ArrayList<>();
		for (TwentyOnePlayer player : this.playerList) {
			BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(tableId, player.getBotUser().getId());
			if (botUser.getScore() > 0) newPlayerList.add(player);
		}
		if (newPlayerList.isEmpty()) {
			try {
				sendMessageManager.sendMessage(BotMessage.simpleTextMessage("游戏结束啦", botMessage));
			} catch (Exception e) {
				log.error("21点提示异常", e);
			}
		}
		this.playerList = newPlayerList;
//		resp.add(BotMessageChain.ofPlain("\n下一局将在1m后开始，未准备将自动离席哦。"));
		this.initData();
		this.waitPeoplePrepare(botMessage);
		return resp;
	}

	public void initData() {
		status = STATUS_WAIT;
		for (TwentyOnePlayer player : playerList) {
			this.initPlayer(player);
		}
		this.cardList = this.newCardList();
	}

	public void initPlayer(TwentyOnePlayer player) {
		player.setCardListList(Lists.newArrayList(
				new TwentyOneCardList()
		));
	}

	private int compareCard(CardResult adminResult, CardResult playerResult, TwentyOneCardList twentyOneCardList) {
		Integer score = twentyOneCardList.getScore();
		String playerSuperCard = playerResult.getSuperCard();
		String adminSuperCard = adminResult.getSuperCard();

		if (Objects.equals(playerSuperCard, BOOM_CARD)) {
			return - score;
		}

		if (playerSuperCard != null && adminSuperCard != null) {
			if (Objects.equals(playerSuperCard, adminSuperCard)) {
				return 0;
			}
		} else if (playerSuperCard == null && adminSuperCard == null) {
			if (playerResult.getSum() == adminResult.getSum()) {
				return 0;
			}
		}

		if (Objects.equals(playerSuperCard, BLACK_JACK)) {
			return score * 3 / 2;
		}

		if (Objects.equals(adminSuperCard, BLACK_JACK)) {
			return - score;
		}

		if (Objects.equals(playerSuperCard, FIVE_CARD)) {
			return score * 3;
		}

		if (Objects.equals(adminSuperCard, FIVE_CARD)) {
			return - score;
		}

		if (Objects.equals(playerSuperCard, BOOM_CARD)) {
			return - score;
		}

		if (Objects.equals(adminSuperCard, BOOM_CARD)) {
			return score;
		}

		if (playerResult.getSum() > adminResult.getSum()) {
			return score;
		} else {
			return - score;
		}
	}

	public CardResult getCardResult(List<TwentyOneCard> cardList) {
		if (cardList.size() == 2) {
			TwentyOneCard card1 = cardList.get(0);
			TwentyOneCard card2 = cardList.get(1);
			if (card1.getPoint() == 1 && card2.getPoint() == 10) {
				return new CardResult(21, 1, BLACK_JACK);
			}
			if (card1.getPoint() == 10 && card2.getPoint() == 1) {
				return new CardResult(21, 1, BLACK_JACK);
			}
		}

		int sum = 0;
		int aCnt = 0;
		for (TwentyOneCard card : cardList) {
			sum += card.getPoint();
			if (card.getPoint() == 1) {
				sum += 10;
				aCnt += 1;
			} else {
				aCnt += 0;
			}
			while (sum > 21 && aCnt > 0) {
				sum -=10;
				aCnt --;
			}
		}

		if (cardList.size() >= 5 && sum <= 21) {
			return new CardResult(sum, aCnt, FIVE_CARD);
		}

		if (sum > 21) {
			return new CardResult(sum, aCnt, BOOM_CARD);
		}
		return new CardResult(sum, aCnt, null);
	}

	public boolean addCard(BotUserDTO botUser) {
		Long playerId = botUser.getId();
		TwentyOnePlayer player = getPlayerByPlayerId(this.getGamingPlayerList(), playerId);
		return this.addCard(player);
	}

	public boolean addCard(TwentyOnePlayer player) {
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.addCard(cardList.remove());
		return true;
	}

	public boolean stopCard(BotUserDTO botUser) {
		Long playerId = botUser.getId();
		TwentyOnePlayer player = getPlayerByPlayerId(this.getGamingPlayerList(), playerId);
		return this.stopCard(player);
	}

	public boolean stopCard(TwentyOnePlayer player) {
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.getFirstNoEndCardList().setStatus(1);
		return true;
	}

	public TwentyOnePlayer getPlayerByPlayerId(Long playerId) {
		return this.getPlayerByPlayerId(this.playerList, playerId);
	}

	public List<TwentyOnePlayer> getGamingPlayerList() {
		return playerList.stream().filter(TwentyOnePlayer::isPrepare).collect(Collectors.toList());
	}

	public TwentyOnePlayer getLastPlayer() {
		return this.getGamingPlayerList().stream().filter(StreamUtil.isNotNull(TwentyOnePlayer::getFirstNoEndCardList)).findFirst().orElse(null);
	}

	public boolean isEnd() {
		return this.getGamingPlayerList().stream().flatMap(player -> player.getCardListList().stream()).map(TwentyOneCardList::getStatus).allMatch(Predicate.isEqual(1));
	}

	public boolean isReady() {
		return playerList.stream().flatMap(player -> player.getCardListList().stream()).map(TwentyOneCardList::getScore).allMatch(Objects::nonNull);
	}

	private TwentyOnePlayer getPlayerByPlayerId(List<TwentyOnePlayer> playerList, Long playerId) {
		return playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
	}

	private boolean needAddCard(List<TwentyOnePlayer> playerList, CardResult cardResult) {
//		int scoreSum = playerList.stream().mapToInt(TwentyOnePlayer::getHaveScore).sum();
//		int scoreAvg = scoreSum / playerList.size();
//		if (scoreAvg <= 100) {
			if (cardResult.getSuperCard() == null) {
				return cardResult.getSum() < 17;
			}
			return false;
//		} else {
//			TwentyOnePlayer firstPlayer = playerList.stream().max(Comparator.comparing(TwentyOnePlayer::getHaveScore)).orElse(null);
//			Asserts.notNull(firstPlayer, "啊嘞，不对劲");
//			int typeCnt = 13;
//			List<TwentyOneCard> adminLastCardList = IntStream.range(0, typeCnt).mapToObj(TwentyOneCard::new).collect(Collectors.toList());
//			List<TwentyOneCard> playerHiddenCardList = IntStream.range(0, typeCnt).mapToObj(TwentyOneCard::new).collect(Collectors.toList());
//
//			long lastExpect = 0;
//			for (TwentyOneCard adminLastCard : adminLastCardList) {
//				List<TwentyOneCard> adminCardList = adminLastCard == null? Lists.newArrayList(): Lists.newArrayList(adminLastCard);
//				adminCardList.addAll(admin.getCardList());
//				for (TwentyOneCard playerHiddenCard : playerHiddenCardList) {
//					List<TwentyOneCard> playerCardList = Lists.newArrayList(playerHiddenCard);
//					playerCardList.addAll(firstPlayer.getCardList().subList(1, firstPlayer.getCardList().size()));
//
//					lastExpect += this.compareCard(this.getCardResult(adminCardList), this.getCardResult(playerCardList), firstPlayer);
//				}
//			}
//
//			long nowExpect = 0;
//			for (TwentyOneCard playerHiddenCard : playerHiddenCardList) {
//				List<TwentyOneCard> playerCardList = Lists.newArrayList(playerHiddenCard);
//				playerCardList.addAll(firstPlayer.getCardList().subList(1, firstPlayer.getCardList().size()));
//
//				nowExpect += this.compareCard(cardResult, this.getCardResult(playerCardList), firstPlayer);
//			}
//
//			return (lastExpect * 1.0 / typeCnt / typeCnt) < (nowExpect * 1.0 / typeCnt);
//		}
	}

	private Queue<TwentyOneCard> newCardList() {
		return IntStream.range(0, 52 * 4).mapToObj(TwentyOneCard::new)
				.map(item -> new SortObject<>(random.nextInt(Integer.MAX_VALUE), item))
				.sorted().map(SortObject::getT).collect(Collectors.toCollection(LinkedList::new));
	}

	public String getStatus() {
		return status;
	}

	public Long getTableId() {
		return tableId;
	}

	public List<TwentyOnePlayer> getPlayerList() {
		return playerList;
	}
}
