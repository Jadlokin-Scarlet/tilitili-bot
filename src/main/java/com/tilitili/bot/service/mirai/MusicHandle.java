package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudOwner;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MusicHandle extends ExceptionRespMessageHandle {
    private final RedisCache redisCache;
    private final MusicService musicService;
    private final BilibiliManager bilibiliManager;
    private final MusicCloudManager musicCloudManager;

    public MusicHandle(RedisCache redisCache, MusicCloudManager musicCloudManager, MusicService musicService, BilibiliManager bilibiliManager) {
        this.redisCache = redisCache;
        this.musicCloudManager = musicCloudManager;
        this.musicService = musicService;
        this.bilibiliManager = bilibiliManager;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        switch (messageAction.getVirtualKeyOrDefault(messageAction.getKeyWithoutPrefix())) {
            case "选歌": return handleChoose(messageAction);
            case "点歌": return handleSearch(messageAction);
            case "切歌": return handleLast(messageAction);
//            case "绑定KTV": return handleBindKTV(messageAction);
            default: throw new AssertException();
        }
    }

//    private BotMessage handleBindKTV(BotMessageAction messageAction) {
//        Long senderId = messageAction.getBotSender().getId();
//        messageAction.get
//        redisCache.setValue("MusicHandle-handleBindKTV-"+senderId, );
//    }

    private BotMessage handleLast(BotMessageAction messageAction) {
        musicService.lastMusic();
        return BotMessage.emptyMessage();
    }

    private BotMessage handleChoose(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        BotSender botSender = messageAction.getBotSender();
        String redisKey = "songList-" + botUser.getId();
        if (!redisCache.exists(redisKey)) {
            return null;
        }
        String value = messageAction.getValueOrVirtualValue();
        Asserts.isNumber(value, "格式错啦(序号)");
        int index = Integer.parseInt(value) - 1;

        List<MusicCloudSong> songList = (List<MusicCloudSong>) redisCache.getValue(redisKey);
        MusicCloudSong song = songList.get(index);
        String owner = song.getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/"));
        String jumpUrl = "https://y.music.163.com/m/song?id=" + song.getId();
        String pictureUrl = song.getAlbum().getPicUrl();
        String musicUrl = "http://music.163.com/song/media/outer/url?sc=wmv&id=" + song.getId();

        redisCache.delete(redisKey);
        musicService.asyncPushVideoAsRTSP(botSender, botUser, song, musicUrl);
        return BotMessage.simpleMusicCloudShareMessage(song.getName(), owner, jumpUrl,pictureUrl, musicUrl);
    }

    private BotMessage handleSearch(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        BotSender botSender = messageAction.getBotSender();
        String searchKey = messageAction.getValue();
        Asserts.notBlank(searchKey, "格式错啦(搜索词)");
        if (Pattern.matches("BV\\w+", searchKey)) {
            return this.handleBilibiliSearch(botSender, botUser, searchKey);
        } else {
            return this.handleMusicCouldSearch(botSender, botUser, searchKey);
        }
    }

    private BotMessage handleBilibiliSearch(BotSender botSender, BotUserDTO botUser, String bv) {
        VideoView videoInfo = bilibiliManager.getVideoInfo(bv);
        String videoUrl = bilibiliManager.getVideoToOSS(bv, videoInfo.getPages().get(0).getCid());
        return BotMessage.simpleVideoMessage(videoInfo.getTitle(), videoUrl);
    }

    private BotMessage handleMusicCouldSearch(BotSender botSender, BotUserDTO botUser, String searchKey) {
        List<MusicCloudSong> songList = musicCloudManager.searchMusicList(searchKey);
        if (songList.size() == 1) {
            MusicCloudSong song = songList.get(0);
            String owner = song.getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/"));
            String jumpUrl = "https://y.music.163.com/m/song?id=" + song.getId();
            String pictureUrl = song.getAlbum().getPicUrl();
            String musicUrl = "http://music.163.com/song/media/outer/url?sc=wmv&id=" + song.getId();

            musicService.asyncPushVideoAsRTSP(botSender, botUser, song, musicUrl);
            return BotMessage.simpleListMessage(Lists.newArrayList(
                    BotMessageChain.ofPlain(String.format("%s\t\t%s\t\t%s", song.getName(), owner, song.getAlbum().getName())),
                    BotMessageChain.ofMusicCloudShare(song.getName(), owner, jumpUrl, pictureUrl, musicUrl)
            ));
        } else {
            String resp = IntStream.range(0, songList.size()).mapToObj(index -> String.format("%s:%s%s\t%s\t%s",
                    index + 1,
                    songList.get(index).getName(),
                    songList.get(index).getFee() == 1? "（VIP）": songList.get(index).getNoCopyrightRcmd() != null? "（下架）": "",
                    songList.get(index).getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/")),
                    songList.get(index).getAlbum().getName()
            )).collect(Collectors.joining("\n"));
            redisCache.setValue("songList-" + botUser.getId(), songList, 60);
            return BotMessage.simpleTextMessage("搜索结果如下，输入序号选择歌曲\n" + resp);
        }
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
