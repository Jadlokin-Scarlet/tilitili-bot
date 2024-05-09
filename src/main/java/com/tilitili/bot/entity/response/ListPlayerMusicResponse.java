package com.tilitili.bot.entity.response;

import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;

public class ListPlayerMusicResponse extends BaseDTO {
	private Long botId;
	private PlayerMusicDTO theMusic;

	public PlayerMusicDTO getTheMusic() {
		return theMusic;
	}

	public ListPlayerMusicResponse setTheMusic(PlayerMusicDTO theMusic) {
		this.theMusic = theMusic;
		return this;
	}
}
