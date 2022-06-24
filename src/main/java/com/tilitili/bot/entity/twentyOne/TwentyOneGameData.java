package com.tilitili.bot.entity.twentyOne;

import java.util.List;
import java.util.Queue;

public class TwentyOneGameData {
	private Queue<String> cardList;
	private List<twentyOnePlayer> table;

	public TwentyOneGameData(Queue<String> cardList, List<twentyOnePlayer> table) {
		this.cardList = cardList;
		this.table = table;
	}

	public Queue<String> getCardList() {
		return cardList;
	}

	public TwentyOneGameData setCardList(Queue<String> cardList) {
		this.cardList = cardList;
		return this;
	}

	public List<twentyOnePlayer> getTable() {
		return table;
	}

	public TwentyOneGameData setTable(List<twentyOnePlayer> table) {
		this.table = table;
		return this;
	}
}
