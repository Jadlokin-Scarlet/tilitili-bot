package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicSongList;

import java.util.List;

public class WebControlDataVO extends BaseDTO {
	private Long botId;
	private Long senderId;
	private String eventToken;
	private PlayerMusicDTO theMusic;
	private List<PlayerMusicDTO> playerQueue;
	private PlayerMusicSongList musicList;
	private Boolean playing;

	public PlayerMusicDTO getTheMusic() {
		return theMusic;
	}

	public WebControlDataVO setTheMusic(PlayerMusicDTO theMusic) {
		this.theMusic = theMusic;
		return this;
	}

	public Long getBotId() {
		return botId;
	}

	public WebControlDataVO setBotId(Long botId) {
		this.botId = botId;
		return this;
	}

	public String getEventToken() {
		return eventToken;
	}

	public WebControlDataVO setEventToken(String eventToken) {
		this.eventToken = eventToken;
		return this;
	}

	public Long getSenderId() {
		return senderId;
	}

	public WebControlDataVO setSenderId(Long senderId) {
		this.senderId = senderId;
		return this;
	}

	public List<PlayerMusicDTO> getPlayerQueue() {
		return playerQueue;
	}

	public WebControlDataVO setPlayerQueue(List<PlayerMusicDTO> playerQueue) {
		this.playerQueue = playerQueue;
		return this;
	}

	public PlayerMusicSongList getMusicList() {
		return musicList;
	}

	public WebControlDataVO setMusicList(PlayerMusicSongList musicList) {
		this.musicList = musicList;
		return this;
	}

	public Boolean getPlaying() {
		return playing;
	}

	public WebControlDataVO setPlaying(Boolean playing) {
		this.playing = playing;
		return this;
	}
}
