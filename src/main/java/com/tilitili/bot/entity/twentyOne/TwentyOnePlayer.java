package com.tilitili.bot.entity.twentyOne;

import java.util.List;

public class TwentyOnePlayer {
	private Integer score;
	private Long playerId;
	private List<String> cardList;

	public TwentyOnePlayer(Long playerId, List<String> cardList) {
		this.playerId = playerId;
		this.cardList = cardList;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public TwentyOnePlayer setPlayerId(Long playerId) {
		this.playerId = playerId;
		return this;
	}

	public List<String> getCardList() {
		return cardList;
	}

	public TwentyOnePlayer setCardList(List<String> cardList) {
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
}
