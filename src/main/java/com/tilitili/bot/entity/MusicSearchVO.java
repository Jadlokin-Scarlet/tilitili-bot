package com.tilitili.bot.entity;

import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.dto.BaseDTO;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;

import java.util.List;

public class MusicSearchVO extends BaseDTO {
	private List<PlayerMusic> playerMusicList;
	private PlayerMusicListDTO playerMusicListDTO;
	private List<MusicCloudSong> songList;

	public List<PlayerMusic> getPlayerMusicList() {
		return playerMusicList;
	}

	public MusicSearchVO setPlayerMusicList(List<PlayerMusic> playerMusicList) {
		this.playerMusicList = playerMusicList;
		return this;
	}

	public PlayerMusicListDTO getPlayerMusicListDTO() {
		return playerMusicListDTO;
	}

	public MusicSearchVO setPlayerMusicListDTO(PlayerMusicListDTO playerMusicListDTO) {
		this.playerMusicListDTO = playerMusicListDTO;
		return this;
	}

	public List<MusicCloudSong> getSongList() {
		return songList;
	}

	public MusicSearchVO setSongList(List<MusicCloudSong> songList) {
		this.songList = songList;
		return this;
	}
}
