package com.tilitili.bot.entity.mirai.event;

public class BotInvitedJoinGroupRequestEvent {
	private String type;
	private Long eventId;
	private String message;
	private Long fromId;
	private Long groupId;
	private String groupName;
	private String nick;

	public String getType() {
		return type;
	}

	public BotInvitedJoinGroupRequestEvent setType(String type) {
		this.type = type;
		return this;
	}

	public Long getEventId() {
		return eventId;
	}

	public BotInvitedJoinGroupRequestEvent setEventId(Long eventId) {
		this.eventId = eventId;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public BotInvitedJoinGroupRequestEvent setMessage(String message) {
		this.message = message;
		return this;
	}

	public Long getFromId() {
		return fromId;
	}

	public BotInvitedJoinGroupRequestEvent setFromId(Long fromId) {
		this.fromId = fromId;
		return this;
	}

	public Long getGroupId() {
		return groupId;
	}

	public BotInvitedJoinGroupRequestEvent setGroupId(Long groupId) {
		this.groupId = groupId;
		return this;
	}

	public String getGroupName() {
		return groupName;
	}

	public BotInvitedJoinGroupRequestEvent setGroupName(String groupName) {
		this.groupName = groupName;
		return this;
	}

	public String getNick() {
		return nick;
	}

	public BotInvitedJoinGroupRequestEvent setNick(String nick) {
		this.nick = nick;
		return this;
	}
}
