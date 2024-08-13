package com.tilitili.bot.entity;

import com.tilitili.common.entity.PlayerMusic;

public class PlayerMusicVO extends PlayerMusic {
	private String jumpUrl;

	public PlayerMusicVO(PlayerMusic other, String jumpUrl) {
		super(other);
		this.jumpUrl = jumpUrl;
	}

	public String getJumpUrl() {
		return jumpUrl;
	}

	public PlayerMusicVO setJumpUrl(String jumpUrl) {
		this.jumpUrl = jumpUrl;
		return this;
	}
}
