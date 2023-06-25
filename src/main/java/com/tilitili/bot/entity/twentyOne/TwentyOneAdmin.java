package com.tilitili.bot.entity.twentyOne;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class TwentyOneAdmin extends BaseDTO {
	private List<TwentyOneCard> cardList;

	@Override
	public String toString() {
		return "琪露诺：" + cardList.stream().map(TwentyOneCard::toString).collect(Collectors.joining(","));
	}

	public void addCard(TwentyOneCard card) {
		this.cardList.add(card);
	}

	public List<TwentyOneCard> getCardList() {
		return cardList;
	}

	public TwentyOneAdmin setCardList(List<TwentyOneCard> cardList) {
		this.cardList = cardList;
		return this;
	}
}
