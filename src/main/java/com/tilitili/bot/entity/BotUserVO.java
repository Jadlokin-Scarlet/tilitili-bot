package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BotUserDTO;

public class BotUserVO extends BotUserDTO {
	private Boolean isAdmin;

	public BotUserVO(BotUserDTO botUser, Boolean isAdmin) {
		this.setId(botUser.getId());
		this.setName(botUser.getName());
		this.setFace(botUser.getFace());
		this.setQq(botUser.getQq());
		this.setTinyId(botUser.getTinyId());
		this.setKookUserId(botUser.getKookUserId());
		this.setQqGuildUserId(botUser.getQqGuildUserId());
		this.setQqMemberOpenid(botUser.getQqMemberOpenid());
		this.isAdmin = isAdmin;
	}

	public Boolean getIsAdmin() {
		return isAdmin;
	}

	public BotUserVO setIsAdmin(Boolean admin) {
		isAdmin = admin;
		return this;
	}
}
