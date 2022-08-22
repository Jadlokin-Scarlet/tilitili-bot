package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUser;

public class FishPlayer {
	public static final Integer STATUS_WAIT = 0;
	public static final Integer STATUS_FISHING = 1;
	public static final Integer STATUS_COLLECT = 2;

	private final BotSender botSender;
	private final BotUser botUser;
	private Integer status;

	public FishPlayer(BotSender botSender, BotUser botUser) {
		this.botSender = botSender;
		this.botUser = botUser;
	}

	public BotSender getBotSender() {
		return botSender;
	}

	public BotUser getBotUser() {
		return botUser;
	}

	public Integer getStatus() {
		return status;
	}

	public FishPlayer setStatus(Integer status) {
		this.status = status;
		return this;
	}
}
