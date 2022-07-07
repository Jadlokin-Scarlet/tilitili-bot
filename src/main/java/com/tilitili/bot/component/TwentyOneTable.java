package com.tilitili.bot.component;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.twentyOne.TwentyOneAdmin;
import com.tilitili.bot.entity.twentyOne.TwentyOneCard;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.bot.service.mirai.PlayTwentyOneHandle;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.SortObject;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwentyOneTable {
	public static final String STATUS_WAIT = "wait";
	public static final String STATUS_PLAYING = "playing";
	public static final String FIVE_CARD = "五龙";
	public static final String BLACK_JACK = "黑杰克";
	private final static Random random = new Random(System.currentTimeMillis());
	private final BotUserMapper botUserMapper;
	private Queue<TwentyOneCard> cardList;
	private List<TwentyOnePlayer> playerList;
	private String status;
	private TwentyOneAdmin admin;

	public TwentyOneTable(BotUserMapper botUserMapper) {
		this.botUserMapper = botUserMapper;
		this.status = STATUS_WAIT;
	}

	public boolean startGame() {
		cardList = this.newCardList();
		admin = new TwentyOneAdmin();
		return true;
	}

	public boolean addGame(BotUser botUser, Integer score) {
		playerList.add(new TwentyOnePlayer().setBotUser(botUser).setScore(score));
		return true;
	}

	public boolean flashCard() {
		TwentyOneCard hiddenCard = cardList.poll();
		Asserts.notNull(hiddenCard, "啊嘞，不对劲。");
		admin.setCardList(Lists.newArrayList(hiddenCard.setHidden(true), cardList.poll()));
		for (TwentyOnePlayer player : playerList) {
			player.setCardList(Lists.newArrayList(cardList.poll(), cardList.poll()));
		}
		return true;
	}

	public List<BotMessageChain> getNoticeMessage() {
		TwentyOnePlayer nowPlayer = this.getLastPlayer();
		// 结算
		if (nowPlayer == null) {
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
			for (TwentyOnePlayer player : playerList) {
				String playerResult = this.getCardResult(player.getCardList(), "player");
				int subScore = this.compareCard(adminCardResult, playerResult, player);
				String playerStr = String.format("%s (%s) (+%d分)%n", player.toString(), playerResult, subScore);
				resp.add(BotMessageChain.ofPlain("\n"));
				resp.add(BotMessageChain.ofPlain(playerStr));
				botUserMapper.updateBotUserSelective(new BotUser().setId(player.getBotUser().getId()).setScore(player.getBotUser().getScore() + subScore));
				PlayTwentyOneHandle.playerLock.remove(player.getPlayerId());
			}
			return resp;
		}

		String adminStr = this.admin.toString() + "\n";
		String playerStr = playerList.stream().map(TwentyOnePlayer::toString).collect(Collectors.joining("\n"));
		List<BotMessageChain> result = Lists.newArrayList(BotMessageChain.ofPlain(adminStr), BotMessageChain.ofPlain(playerStr));
		result.addAll(Arrays.asList(BotMessageChain.ofAt(nowPlayer.getBotUser().getExternalId()), BotMessageChain.ofPlain(
				"请选择：加牌、停牌、加倍(加倍积分并加牌)"
		)));
		return result;
	}

	private int compareCard(String adminCardResult, String playerResult, TwentyOnePlayer player) {
		Integer score = player.getScore();
		if (Objects.equals(playerResult, adminCardResult)) {
			return score;
		}

		if (playerResult.equals(FIVE_CARD)) {
			return score * 3;
		}

		if (playerResult.equals(BLACK_JACK)) {
			return score * 5 / 2;
		}

		if (adminCardResult.equals(BLACK_JACK)) {
			return 0;
		}

		if (playerResult.compareTo(adminCardResult) > 0) {
			return score * 2;
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
			aCnt += card.getPoint() == 1? 1: 0;
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

		return String.format("%02d", sum);
	}

	public boolean addCard(BotUser botUser) {
		Long playerId = botUser.getExternalId();
		TwentyOnePlayer player = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.addCard(cardList.poll());
		return true;
	}

	public boolean stopCard(BotUser botUser) {
		Long playerId = botUser.getExternalId();
		TwentyOnePlayer player = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
		Asserts.notNull(player, "啊嘞，有点不对劲");
		player.setStatus(1);
		return true;
	}

	public boolean doubleAddCard(BotUser botUser) {
		Long playerId = botUser.getExternalId();
		Integer hasScore = botUser.getScore();
		TwentyOnePlayer player = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getPlayerId, playerId)).findFirst().orElse(null);
		Asserts.notNull(player, "啊嘞，有点不对劲");
		Asserts.isFalse(player.getIsDouble(), "你已经双倍过了，不能再双倍了哦");
		Integer useScore = player.getScore();
		Asserts.isTrue(useScore <= hasScore, "积分好像不够惹。");
		player.setScore(useScore * 2);
		botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(hasScore - useScore));
		player.addCard(cardList.poll());
		return true;
	}


	public TwentyOnePlayer getLastPlayer() {
		return playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getStatus, 0)).findFirst().orElse(null);
	}

	private Queue<TwentyOneCard> newCardList() {
		return IntStream.range(0, 208).mapToObj(TwentyOneCard::new)
				.map(item -> new SortObject<>(random.nextInt(Integer.MAX_VALUE), item))
				.sorted().map(SortObject::getT).collect(Collectors.toCollection(LinkedList::new));
	}

	public boolean isReady() {
		return playerList.stream().map(TwentyOnePlayer::getScore).allMatch(Objects::nonNull);
	}

	public String getStatus() {
		return status;
	}
}
