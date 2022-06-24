package com.tilitili.bot.entity.twentyOne;

import java.util.List;

public class twentyOnePlayer {
	private Long playerId;
	private List<String> cardList;

	public twentyOnePlayer(Long playerId, List<String> cardList) {
		this.playerId = playerId;
		this.cardList = cardList;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public twentyOnePlayer setPlayerId(Long playerId) {
		this.playerId = playerId;
		return this;
	}

	public List<String> getCardList() {
		return cardList;
	}

	public twentyOnePlayer setCardList(List<String> cardList) {
		this.cardList = cardList;
		return this;
	}
}
