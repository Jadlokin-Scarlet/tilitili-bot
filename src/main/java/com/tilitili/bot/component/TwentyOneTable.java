package com.tilitili.bot.component;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.twentyOne.TwentyOneAdmin;
import com.tilitili.bot.entity.twentyOne.TwentyOneCard;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
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
		String adminStr = this.admin.toString() + "\n";
		String playerStr = playerList.stream().map(TwentyOnePlayer::toString).collect(Collectors.joining("\n"));
		List<BotMessageChain> result = Lists.newArrayList(BotMessageChain.ofPlain(adminStr), BotMessageChain.ofPlain(playerStr));

		Optional<TwentyOnePlayer> nowPlayerOptional = playerList.stream().filter(StreamUtil.isEqual(TwentyOnePlayer::getStatus, 0)).findFirst();
		if (!nowPlayerOptional.isPresent()) {
			// TODO
			return null;
		}
		TwentyOnePlayer nowPlayer = nowPlayerOptional.get();
		result.addAll(Arrays.asList(BotMessageChain.ofAt(nowPlayer.getBotUser().getExternalId()), BotMessageChain.ofPlain(
				"请选择：加牌、停牌、加倍(加倍积分并加牌)"
		)));
		return result;
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
