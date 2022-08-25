package com.tilitili.bot.entity.mirai.event;

import com.tilitili.common.entity.view.bot.mirai.MiraiFriend;

public class FriendInputStatusChangedEvent {
	private String type;
	private MiraiFriend friend;
	private Boolean inputting;

	public String getType() {
		return type;
	}

	public FriendInputStatusChangedEvent setType(String type) {
		this.type = type;
		return this;
	}

	public MiraiFriend getFriend() {
		return friend;
	}

	public FriendInputStatusChangedEvent setFriend(MiraiFriend friend) {
		this.friend = friend;
		return this;
	}

	public Boolean getInputting() {
		return inputting;
	}

	public FriendInputStatusChangedEvent setInputting(Boolean inputting) {
		this.inputting = inputting;
		return this;
	}
}
