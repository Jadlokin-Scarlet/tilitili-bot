package com.tilitili.bot.entity.mirai.event;

public class NewFriendRequestEvent {
	private String type;
	private Long eventId;
	private Long fromId;
	private Long groupId;
	private String nick;
	private String message;

	public String getType() {
		return type;
	}

	public NewFriendRequestEvent setType(String type) {
		this.type = type;
		return this;
	}

	public Long getEventId() {
		return eventId;
	}

	public NewFriendRequestEvent setEventId(Long eventId) {
		this.eventId = eventId;
		return this;
	}

	public Long getFromId() {
		return fromId;
	}

	public NewFriendRequestEvent setFromId(Long fromId) {
		this.fromId = fromId;
		return this;
	}

	public Long getGroupId() {
		return groupId;
	}

	public NewFriendRequestEvent setGroupId(Long groupId) {
		this.groupId = groupId;
		return this;
	}

	public String getNick() {
		return nick;
	}

	public NewFriendRequestEvent setNick(String nick) {
		this.nick = nick;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public NewFriendRequestEvent setMessage(String message) {
		this.message = message;
		return this;
	}
}
