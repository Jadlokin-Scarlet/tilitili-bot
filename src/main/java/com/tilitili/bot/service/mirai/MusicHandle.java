package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.manager.MusicCloudManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MusicHandle extends ExceptionRespMessageHandle {
    private final MusicCloudManager musicCloudManager;

    public MusicHandle(MusicCloudManager musicCloudManager) {
        this.musicCloudManager = musicCloudManager;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        String searchKey = messageAction.getValue();
        List<MusicCloudSong> songList = musicCloudManager.searchMusicList(searchKey);

        return null;
    }
}
