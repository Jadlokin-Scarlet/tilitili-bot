package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.BotUser;

import java.util.List;
import java.util.stream.Collectors;

public class TwentyOnePlayer {
	private BotUser botUser;
	private Integer score;
	private List<TwentyOneCard> cardList;
	private int status;

	public TwentyOnePlayer() {
		status = 0;
	}

	public TwentyOnePlayer(BotUser botUser, List<TwentyOneCard> cardList) {
		this.botUser = botUser;
		this.cardList = cardList;
		status = 0;
	}

	@Override
	public String toString() {
		return String.format("%s：%s", botUser.getName(), cardList.stream().map(TwentyOneCard::toString).collect(Collectors.joining(",")));
	}

	public List<TwentyOneCard> getCardList() {
		return cardList;
	}

	public TwentyOnePlayer setCardList(List<TwentyOneCard> cardList) {
		this.cardList = cardList;
		return this;
	}

	public Integer getScore() {
		return score;
	}

	public TwentyOnePlayer setScore(Integer score) {
		this.score = score;
		return this;
	}

	public BotUser getBotUser() {
		return botUser;
	}

	public TwentyOnePlayer setBotUser(BotUser botUser) {
		this.botUser = botUser;
		return this;
	}

	public int getStatus() {
		return status;
	}

	public TwentyOnePlayer setStatus(int status) {
		this.status = status;
		return this;
	}
}
