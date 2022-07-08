package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.BotUser;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TwentyOnePlayer {
	private BotUser botUser;
	private Integer score;
	private List<TwentyOneCard> cardList;
	private int status;
	private boolean isDouble;

	public TwentyOnePlayer() {
		status = 0;
		isDouble = false;
	}

	public TwentyOnePlayer(BotUser botUser, List<TwentyOneCard> cardList) {
		this();
		this.botUser = botUser;
		this.cardList = cardList;
	}

	@Override
	public String toString() {
		return String.format("%s：%s", botUser.getName(), cardList.stream().map(TwentyOneCard::toString).collect(Collectors.joining(",")));
	}

	public Long getPlayerId() {
		return botUser == null? null: botUser.getExternalId();
	}

	public void addCard(TwentyOneCard card) {
		this.cardList.add(card);
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

	public boolean getIsDouble() {
		return isDouble;
	}

	public TwentyOnePlayer setIsDouble(boolean isDouble) {
		this.isDouble = isDouble;
		return this;
	}

	public boolean needEnd(String cardResult) {
		return !NumberUtils.isDigits(cardResult) || Objects.equals(cardResult, "21");
	}
}
