package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicSongList;

import java.util.List;

public class MusicSearchKeyHandleResult {
    private List<PlayerMusicDTO> playerMusicList;
    private PlayerMusicSongList playerMusicSongList;


    public PlayerMusicSongList getPlayerMusicSongList() {
        return playerMusicSongList;
    }

    public MusicSearchKeyHandleResult setPlayerMusicSongList(PlayerMusicSongList playerMusicSongList) {
        this.playerMusicSongList = playerMusicSongList;
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
