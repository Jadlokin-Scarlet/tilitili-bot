package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class TwentyOneCardList extends BaseDTO {
	private Integer score;
	private List<TwentyOneCard> cardList;
	private int status;

	public TwentyOneCardList() {
		status = 0;
	}

	public void addCard(TwentyOneCard card) {
		this.cardList.add(card);
	}


	public List<TwentyOneCard> getCardList() {
		return cardList;
	}

	public TwentyOneCardList setCardList(List<TwentyOneCard> cardList) {
		this.cardList = cardList;
		return this;
	}

	public Integer getScore() {
		return score;
	}

	public TwentyOneCardList setScore(Integer score) {
		this.score = score;
		return this;
	}

	public int getStatus() {
		return status;
	}

	public TwentyOneCardList setStatus(int status) {
		this.status = status;
		return this;
	}

	public boolean needEnd(CardResult cardResult) {
		return cardResult.getSuperCard() != null || (cardResult.getSum() == 21 && cardResult.getaCnt() == 0);
	}
}
