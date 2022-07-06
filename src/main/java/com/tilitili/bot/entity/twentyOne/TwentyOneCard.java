package com.tilitili.bot.entity.twentyOne;

import java.util.Arrays;
import java.util.List;

public class TwentyOneCard {
	private String type;
	private String value;
	private boolean hidden;
	private static final List<String> CARD_TYPE_LIST = Arrays.asList("♤️", "♧", "♢", "♡");
	private static final List<String> CARD_VALUE_LIST = Arrays.asList("A","2","3","4","5","6","7","8","9","10","J","Q","K");

	public TwentyOneCard(Integer code) {
		this(code, false);
	}

	public TwentyOneCard(Integer code, boolean hidden) {
		type = CARD_TYPE_LIST.get((code % 52) / 13);
		value = CARD_VALUE_LIST.get(code % 13);
		this.hidden = hidden;
	}

	@Override
	public String toString() {
		if (hidden) return "*";
		return type + value;
	}







	public String getType() {
		return type;
	}

	public TwentyOneCard setType(String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public TwentyOneCard setValue(String value) {
		this.value = value;
		return this;
	}

	public boolean getHidden() {
		return hidden;
	}

	public TwentyOneCard setHidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}
}
