package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudOwner;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MusicHandle extends ExceptionRespMessageHandle {
    private final RedisCache redisCache;
    private final MusicCloudManager musicCloudManager;

    public MusicHandle(RedisCache redisCache, MusicCloudManager musicCloudManager) {
        this.redisCache = redisCache;
        this.musicCloudManager = musicCloudManager;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        switch (messageAction.getVirtualKeyOrDefault(messageAction.getKeyWithoutPrefix())) {
            case "点歌": return handleSearch(messageAction);
            case "选歌": return handleChoose(messageAction);
            default: throw new AssertException();
        }
    }

    private BotMessage handleChoose(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        String redisKey = "songList-" + botUser.getId();
        if (!redisCache.exists(redisKey)) {
            return null;
        }
        String value = messageAction.getValueOrVirtualValue();
        Asserts.isNumber(value, "格式错啦(序号)");
        int index = Integer.parseInt(value) - 1;

        List<MusicCloudSong> songList = (List<MusicCloudSong>) redisCache.getValue(redisKey);
        MusicCloudSong song = songList.get(index);
        return BotMessage.simpleTextMessage(String.format("%s\t%s\t%s",
                song.getName(),
                song.getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/")),
                song.getAlbum().getName()));
    }

    private BotMessage handleSearch(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        String searchKey = messageAction.getValue();
        List<MusicCloudSong> songList = musicCloudManager.searchMusicList(searchKey);
        String resp = IntStream.range(0, songList.size()).mapToObj(index -> String.format("%s:%s\t%s\t%s",
                index + 1,
                songList.get(index).getName(),
                songList.get(index).getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/")),
                songList.get(index).getAlbum().getName()
        )).collect(Collectors.joining("\n"));
        redisCache.setValue("songList-"+botUser.getId(), songList, 60);
        return BotMessage.simpleTextMessage("搜索结果如下，输入序号选择歌曲\n" + resp);
    }

    @Override
    public String isThisTask(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        if (redisCache.exists("songList-"+botUser.getId()) && StringUtils.isNumber(messageAction.getText())) {
            return "选歌";
        }
        return null;
    }
}
