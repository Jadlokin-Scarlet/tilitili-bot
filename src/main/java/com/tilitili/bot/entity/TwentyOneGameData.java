package com.tilitili.bot.entity;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class TwentyOneGameData {
	private Queue<String> cardList;
	private Map<String, List<String>> table;

	public TwentyOneGameData(Queue<String> cardList, Map<String, List<String>> table) {
		this.cardList = cardList;
		this.table = table;
	}

	public void getShowTable() {
//		table.entrySet().stream().flatMap(entry -> Stream.of(
//				BotMessageChain.ofAt(entry.getKey())
//		))
//
//
//		return Arrays.asList(
//				BotMessageChain.ofAt()
//		)
	}



	public Queue<String> getCardList() {
		return cardList;
	}

	public TwentyOneGameData setCardList(Queue<String> cardList) {
		this.cardList = cardList;
		return this;
	}

	public Map<String, List<String>> getTable() {
		return table;
	}

	public TwentyOneGameData setTable(Map<String, List<String>> table) {
		this.table = table;
		return this;
	}
}
