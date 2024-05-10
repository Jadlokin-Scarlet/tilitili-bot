package com.tilitili.bot.entity.response;

import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;

public class ListPlayerMusicResponse extends BaseDTO {
	private Long botId;
	private PlayerMusicDTO theMusic;
	private String eventToken;

	public PlayerMusicDTO getTheMusic() {
		return theMusic;
	}

	public ListPlayerMusicResponse setTheMusic(PlayerMusicDTO theMusic) {
		this.theMusic = theMusic;
		return this;
	}

	public Long getBotId() {
		return botId;
	}

	public ListPlayerMusicResponse setBotId(Long botId) {
		this.botId = botId;
		return this;
	}

	public String getEventToken() {
		return eventToken;
	}

	public ListPlayerMusicResponse setEventToken(String eventToken) {
		this.eventToken = eventToken;
		return this;
	}
}
