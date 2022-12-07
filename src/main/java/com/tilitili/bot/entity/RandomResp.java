package com.tilitili.bot.entity;

import java.util.concurrent.ThreadLocalRandom;

public class RandomResp {
	private final String[] respList;

	public RandomResp(String... respList) {
		this.respList = respList;
	}

	public String getResp() {
		if (respList.length == 0) return null;
		return respList[ThreadLocalRandom.current().nextInt(respList.length)];
	}
}
