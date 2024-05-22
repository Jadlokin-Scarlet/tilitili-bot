package com.tilitili.bot.entity;

import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;

import java.util.List;

public class WebControlDataVO extends BaseDTO {
	private PlayerMusic theMusic;
	private List<PlayerMusic> playerQueue;
	private PlayerMusicListDTO musicList;
	private Boolean playing;

	public PlayerMusic getTheMusic() {
		return theMusic;
	}

	public WebControlDataVO setTheMusic(PlayerMusic theMusic) {
		this.theMusic = theMusic;
		return this;
	}

	public List<PlayerMusic> getPlayerQueue() {
		return playerQueue;
	}

	public WebControlDataVO setPlayerQueue(List<PlayerMusic> playerQueue) {
		this.playerQueue = playerQueue;
		return this;
	}

	public PlayerMusicListDTO getMusicList() {
		return musicList;
	}

	public WebControlDataVO setMusicList(PlayerMusicListDTO musicList) {
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
