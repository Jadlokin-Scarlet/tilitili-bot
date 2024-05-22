package com.tilitili.bot.entity;

import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;

import java.util.List;

public class MusicSearchKeyHandleResult {
    private List<PlayerMusic> playerMusicList;
    private PlayerMusicListDTO playerMusicListDTO;


    public PlayerMusicListDTO getPlayerMusicListDTO() {
        return playerMusicListDTO;
    }

    public MusicSearchKeyHandleResult setPlayerMusicListDTO(PlayerMusicListDTO playerMusicListDTO) {
        this.playerMusicListDTO = playerMusicListDTO;
        return this;
    }

    public List<PlayerMusic> getPlayerMusicList() {
        return playerMusicList;
    }

    public MusicSearchKeyHandleResult setPlayerMusicList(List<PlayerMusic> playerMusicList) {
        this.playerMusicList = playerMusicList;
        return this;
    }
}
