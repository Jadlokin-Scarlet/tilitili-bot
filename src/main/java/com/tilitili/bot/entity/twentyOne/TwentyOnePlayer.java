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

	@Override
	public String toString() {
		return String.format("%s：%s", botUser.getName(), cardList.stream().map(TwentyOneCard::toString).collect(Collectors.joining(",")));
	}

	public Long getPlayerId() {
		return botUser == null? null: botUser.getExternalId();
	}

	public Integer getHaveScore() {
		return botUser == null? null: botUser.getScore();
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

	public boolean needEnd(CardResult cardResult) {
		return cardResult.getSuperCard() != null || (cardResult.getSum() == 21 && cardResult.getaCnt() == 0);
	}
}
