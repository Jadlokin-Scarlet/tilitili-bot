package com.tilitili.bot.entity.twentyOne;

import java.util.Arrays;
import java.util.List;

public class TwentyOneCard {
	private final int code;
	private final String type;
	private final String value;
	private boolean hidden;
	private static final List<String> CARD_TYPE_LIST = Arrays.asList("♤️", "♧", "♢", "♡");
	private static final List<String> CARD_VALUE_LIST = Arrays.asList("A","2","3","4","5","6","7","8","9","10","J","Q","K");
	private static final List<Integer> CARD_POINT_LIST = Arrays.asList(1,2,3,4,5,6,7,8,9,10,10,10,10);

	public TwentyOneCard(int code) {
		this(code, false);
	}

	public TwentyOneCard(int code, boolean hidden) {
		this.code = code;
		type = CARD_TYPE_LIST.get((code % 52) / 13);
		value = CARD_VALUE_LIST.get(code % 13);
		this.hidden = hidden;
	}

	@Override
	public String toString() {
		if (hidden) return "**";
		return type + value;
	}

	public Integer getPoint() {
		return CARD_POINT_LIST.get(code % 13);
	}





	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public boolean getHidden() {
		return hidden;
	}

	public TwentyOneCard setHidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}
}
