package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BotUserDTO;

public class BotUserVO extends BotUserDTO {
	private Boolean isAdmin;

	public BotUserVO(BotUserDTO botUser, Boolean isAdmin) {
		this.setId(botUser.getId());
		this.setName(botUser.getName());
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
