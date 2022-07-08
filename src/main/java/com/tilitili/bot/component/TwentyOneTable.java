package com.tilitili.bot.component;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.twentyOne.TwentyOneAdmin;
import com.tilitili.bot.entity.twentyOne.TwentyOneCard;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.SortObject;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwentyOneTable {
	public static final String STATUS_WAIT = "wait";
	public static final String STATUS_PLAYING = "playing";
	public static final String FIVE_CARD = "五龙";
	public static final String BLACK_JACK = "黑杰克";
	public static final String BOOM_CARD = "爆牌";
	private final static Random random = new Random(System.currentTimeMillis());
	private final BotUserMapper botUserMapper;
	private final Long tableId;
	private String status;
	private final List<TwentyOnePlayer> playerList;
	private final Queue<TwentyOneCard> cardList;
	private final TwentyOneAdmin admin;

	public TwentyOneTable(BotUserMapper botUserMapper, Long tableId) {
		this.botUserMapper = botUserMapper;
		this.tableId = tableId;
		this.status = STATUS_WAIT;
		this.playerList = new ArrayList<>();
		this.cardList = this.newCardList();
		this.admin = new TwentyOneAdmin();
	}

	public boolean addGame(BotUser botUser, Integer score) {
		TwentyOnePlayer player = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, botUser.getExternalId())).findFirst().orElse(null);
		if (player == null) {
			playerList.add(new TwentyOnePlayer().setBotUser(botUser).setScore(score));
		} else {
			player.setScore(score);
		}
		return true;
	}

	public boolean addGame(BotUser botUser) {
		TwentyOnePlayer player = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, botUser.getExternalId())).findFirst().orElse(null);
		if (player == null) {
			playerList.add(new TwentyOnePlayer().setBotUser(botUser));
			return true;
		} else {
			return false;
		}
	}

	public boolean removePlayer(Long playerId) {
		int playerIndex = -1;
		for (int index = 0, playerListSize = playerList.size(); index < playerListSize; index++) {
			TwentyOnePlayer player = playerList.get(index);
			if (Objects.equals(player.getPlayerId(), playerId)) playerIndex = index;
		}
		Asserts.notEquals(playerIndex, -1, "啊嘞，好像不对劲");
		playerList.remove(playerIndex);

		return true;
	}

	public boolean flashCard() {
		TwentyOneCard hiddenCard = cardList.poll();
		Asserts.notNull(hiddenCard, "啊嘞，不对劲。");
		admin.setCardList(Lists.newArrayList(hiddenCard.setHidden(true), cardList.poll()));
		for (TwentyOnePlayer player : this.getGamingPlayerList()) {
			player.setCardList(Lists.newArrayList(cardList.poll(), cardList.poll()));
		}
		status = STATUS_PLAYING;
		return true;
	}

	public List<BotMessageChain> getWaitMessage() {
		List<String> allPlayer = new ArrayList<>();
		List<String> preparePlayer = new ArrayList<>();
		List<String> notPreparePlayer = new ArrayList<>();
		for (TwentyOnePlayer player : playerList) {
			String name = player.getBotUser().getName();
			allPlayer.add(name);
			if (player.getScore() == null) {
				notPreparePlayer.add(name);
			} else {
				preparePlayer.add(name);
			}
		}
		String message = String.format("此桌参与人员：%s\n已准备：%s\n未准备：%s\n", String.join(",", allPlayer), String.join(",", preparePlayer), String.join(",", notPreparePlayer));
		return Lists.newArrayList(BotMessageChain.ofPlain(message));
	}

	public List<BotMessageChain> getNoticeMessage(String sendType) {
		TwentyOnePlayer nowPlayer = this.getLastPlayer();
		if (nowPlayer.needEnd(this.getCardResult(nowPlayer.getCardList(), "player"))) {
			this.stopCard(nowPlayer);
			if (this.isEnd()) return this.getEndMessage();
			nowPlayer = this.getLastPlayer();
		}

		Asserts.notNull(nowPlayer, "啊嘞，似乎不对劲");

		String adminStr = this.admin.toString() + "\n";
		String playerStr = this.getGamingPlayerList().stream().map(TwentyOnePlayer::toString).collect(Collectors.joining("\n"));
		List<BotMessageChain> result = Lists.newArrayList(BotMessageChain.ofPlain(adminStr), BotMessageChain.ofPlain(playerStr));
		result.add(BotMessageChain.ofPlain("\n"));
		if (!Objects.equals(sendType, SendTypeEmum.FRIEND_MESSAGE_STR)) {
			result.add(BotMessageChain.ofAt(nowPlayer.getBotUser().getExternalId()));
		}
		result.add(BotMessageChain.ofPlain("请选择：加牌、停牌、加倍(加倍积分并加牌)"));
		return result;
	}

	// 结算
	public List<BotMessageChain> getEndMessage() {
		// 庄家翻牌
		admin.getCardList().get(0).setHidden(false);

		// 抽牌
		String adminCardResult;
		while (admin.needAddCard(adminCardResult = this.getCardResult(admin.getCardList(), "admin"))) {
			admin.addCard(cardList.poll());
		}
		List<BotMessageChain> resp = new ArrayList<>();
		String adminStr = String.format("%s (%s)", admin.toString(), adminCardResult);
		resp.add(BotMessageChain.ofPlain(adminStr));
		// 对比
		for (TwentyOnePlayer player : this.getGamingPlayerList()) {
			String playerResult = this.getCardResult(player.getCardList(), "player");
			int subScore = this.compareCard(adminCardResult, playerResult, player);
			String playerStr = String.format("%s (%s) (%s分)%n", player.toString(), playerResult, subScore > 0? "+" + subScore: "" + subScore);
			resp.add(BotMessageChain.ofPlain("\n"));
			resp.add(BotMessageChain.ofPlain(playerStr));
			BotUser botUser = botUserMapper.getBotUserByExternalId(player.getPlayerId());
			botUserMapper.updateBotUserSelective(new BotUser().setId(player.getBotUser().getId()).setScore(botUser.getScore() + subScore + player.getScore()));
		}
		status = STATUS_WAIT;
		for (TwentyOnePlayer player : playerList) {
			player.setScore(null);
			player.setStatus(0);
			player.setCardList(null);
			player.setIsDouble(false);
		}
		return resp;
	}

	private int compareCard(String adminResult, String playerResult, TwentyOnePlayer player) {
		Integer score = player.getScore();
		if (Objects.equals(playerResult, adminResult)) {
			return score;
		}

		if (playerResult.equals(FIVE_CARD)) {
			return score * 3;
		}

		if (playerResult.equals(BLACK_JACK)) {
			return score * 3 / 2;
		}

		if (adminResult.equals(BLACK_JACK)) {
			return - score * 3 / 2;
		}

		if (playerResult.equals(BOOM_CARD)) {
			return - score;
		}

		if (adminResult.equals(BOOM_CARD)) {
			return score;
		}

		if (playerResult.compareTo(adminResult) > 0) {
			return score;
		} else {
			return 0;
		}
	}

	public String getCardResult(List<TwentyOneCard> cardList, String type) {
		if (cardList.size() == 2) {
			TwentyOneCard card1 = cardList.get(0);
			TwentyOneCard card2 = cardList.get(1);
			if (card1.getPoint() == 1 && card2.getPoint() == 10) {
				return BLACK_JACK;
			}
			if (card1.getPoint() == 10 && card2.getPoint() == 1) {
				return BLACK_JACK;
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
			if (sum > 21 && aCnt > 0) {
				sum -=10;
				aCnt --;
			}
		}

		if (type.equals("player")) {
			if (cardList.size() >= 5 && sum <= 21) {
				return FIVE_CARD;
			}
		}

		if (sum > 21) {
			return BOOM_CARD;
		}

		return String.format("%02d", sum);
	}

	public boolean addCard(BotUser botUser) {
		Long playerId = botUser.getExternalId();
		TwentyOnePlayer player = this.getGamingPlayerList().stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
		return this.addCard(player);
	}

	public boolean addCard(TwentyOnePlayer player) {
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.addCard(cardList.poll());
		return true;
	}

	public boolean stopCard(BotUser botUser) {
		Long playerId = botUser.getExternalId();
		TwentyOnePlayer player = this.getGamingPlayerList().stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
		return this.stopCard(player);
	}

	public boolean stopCard(TwentyOnePlayer player) {
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.setStatus(1);
		return true;
	}

	public TwentyOnePlayer getPlayer(Long playerId) {
		return playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
	}

	public List<TwentyOnePlayer> getGamingPlayerList() {
		return playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getScore, null).negate()).collect(Collectors.toList());
	}

	public TwentyOnePlayer getLastPlayer() {
		return this.getGamingPlayerList().stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getStatus, 0)).findFirst().orElse(null);
	}

	private Queue<TwentyOneCard> newCardList() {
		return IntStream.range(0, 208).mapToObj(TwentyOneCard::new)
				.map(item -> new SortObject<>(random.nextInt(Integer.MAX_VALUE), item))
				.sorted().map(SortObject::getT).collect(Collectors.toCollection(LinkedList::new));
	}

	public boolean isEnd() {
		return this.getGamingPlayerList().stream().map(TwentyOnePlayer::getStatus).allMatch(Predicate.isEqual(1));
	}

	public boolean isReady() {
		return playerList.stream().map(TwentyOnePlayer::getScore).allMatch(Objects::nonNull);
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
