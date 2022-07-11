package com.tilitili.bot.entity.twentyOne;

public class CardResult {
	private final int sum;
	private final int aCnt;
	private final String superCard;

	public CardResult(int sum, int aCnt, String superCard) {
		this.sum = sum;
		this.aCnt = aCnt;
		this.superCard = superCard;
	}

	public int getSum() {
		return sum;
	}

	public int getaCnt() {
		return aCnt;
	}

	public String getSuperCard() {
		return superCard;
	}
}
