package com.tilitili.bot.component;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.twentyOne.TwentyOnePlayer;
import com.tilitili.common.entity.SortObject;
import com.tilitili.common.mapper.mysql.BotUserMapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwentyOneTable {
	public static final String STATUS_WAIT = "wait";
	public static final String STATUS_PLAYING = "playing";
	private final List<String> cardTypeList = Arrays.asList("♤️", "♧", "♢", "♡");
	private final static Random random = new Random(System.currentTimeMillis());
	private final BotUserMapper botUserMapper;
	private Queue<String> cardList;
	private List<TwentyOnePlayer> playerList;
	private String status;

	public TwentyOneTable(BotUserMapper botUserMapper) {
		this.botUserMapper = botUserMapper;
		this.status = STATUS_WAIT;
	}

	public boolean startGame(Long playerId) {
		cardList = this.newCardList();
		playerList = Lists.newArrayList(
				new TwentyOnePlayer(playerId, Lists.newArrayList(cardList.poll(), cardList.poll()))
		);
		return true;
	}

	public boolean addGame(Long playerId, Integer score) {
		playerList.add(new TwentyOnePlayer(playerId, Lists.newArrayList(cardList.poll(), cardList.poll())));
		return true;
	}

	private Queue<String> newCardList() {
		return IntStream.range(0, 208).mapToObj(index -> cardTypeList.get((index % 52) / 13) + "" + index % 13)
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
