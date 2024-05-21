package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;

import java.util.List;

public class MusicSearchKeyHandleResult {
    private List<PlayerMusicDTO> playerMusicList;
    private PlayerMusicListDTO playerMusicListDTO;


    public PlayerMusicListDTO getPlayerMusicSongList() {
        return playerMusicListDTO;
    }

    public MusicSearchKeyHandleResult setPlayerMusicSongList(PlayerMusicListDTO playerMusicListDTO) {
        this.playerMusicListDTO = playerMusicListDTO;
        return this;
    }

    public List<PlayerMusicDTO> getPlayerMusicList() {
        return playerMusicList;
    }

    public MusicSearchKeyHandleResult setPlayerMusicList(List<PlayerMusicDTO> playerMusicList) {
        this.playerMusicList = playerMusicList;
        return this;
    }
}
