package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusic;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudOwner;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudProgram;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MusicHandle extends ExceptionRespMessageHandle {
    private final RedisCache redisCache;
    private final MusicService musicService;
    private final BilibiliManager bilibiliManager;
    private final MusicCloudManager musicCloudManager;
    private final AtomicBoolean lockFlag = new AtomicBoolean(false);

    public MusicHandle(RedisCache redisCache, MusicCloudManager musicCloudManager, MusicService musicService, BilibiliManager bilibiliManager) {
        this.redisCache = redisCache;
        this.musicService = musicService;
        this.bilibiliManager = bilibiliManager;
        this.musicCloudManager = musicCloudManager;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        try {
            Asserts.isTrue(lockFlag.compareAndSet(false, true), "猪脑过载，你先别急 Σ（ﾟдﾟlll）");
            switch (messageAction.getVirtualKeyOrDefault(messageAction.getKeyWithoutPrefix())) {
                case "选歌": return handleChoose(messageAction);
                case "点歌": return handleSearch(messageAction);
                case "切歌": return handleLast(messageAction);
    //            case "绑定KTV": return handleBindKTV(messageAction);
                case "停止": return handleStop(messageAction);
                case "继续": return handleStart(messageAction);
                case "播放列表": return handleList(messageAction);
                case "循环播放": return handleLoopPlayer(messageAction);
                default: throw new AssertException();
            }
        } finally {
            lockFlag.set(false);
        }
    }

    private BotMessage handleLoopPlayer(BotMessageAction messageAction) {
        Boolean success = musicService.loopPlayer(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (success == null) {
            return null;
        }
        if (success) {
            return BotMessage.simpleTextMessage("(仅)当前歌曲将会循环播放，发送切歌切换下一首。");
        } else {
            throw new AssertException();
        }
    }

    private BotMessage handleList(BotMessageAction messageAction) {
        List<PlayerMusic> playerMusicList = musicService.listMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.isEmpty()) {
            return BotMessage.simpleTextMessage("播放列表空空如也。");
        } else {
            List<String> respList = new ArrayList<>();
            for (int index = 0; index < playerMusicList.size(); index++) {
                PlayerMusic playerMusic = playerMusicList.get(index);
                String indexStr = index == 0? "当前": index == 1? "下一首": String.valueOf(index);
                respList.add(String.format("%s：%s", indexStr, playerMusic.getName()));
            }
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
    }

    private BotMessage handleStart(BotMessageAction messageAction) {
        List<PlayerMusic> playerMusicList = musicService.startMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.isEmpty()) {
            return BotMessage.simpleTextMessage("播放列表空空如也。");
        } else {
            return BotMessage.emptyMessage();
        }
    }

    private BotMessage handleStop(BotMessageAction messageAction) {
        List<PlayerMusic> playerMusicList = musicService.stopMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.size() < 2) {
            return BotMessage.simpleTextMessage("已停止，无下一首，点歌以继续。");
        }
        return BotMessage.simpleTextMessage(String.format("已停止，输入继续播放下一首：[%s]。", playerMusicList.get(1).getName()));
    }

    private BotMessage handleLast(BotMessageAction messageAction) {
        List<PlayerMusic> playerMusicList = musicService.lastMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.size() < 2) {
            return BotMessage.simpleTextMessage("播放列表空空如也。");
        }
        return BotMessage.emptyMessage();
    }

    private BotMessage handleChoose(BotMessageAction messageAction) throws IOException {
        BotRobot bot = messageAction.getBot();
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
        List<PlayerMusic> playerMusicList = musicService.pushVideoToQuote(bot, botSender, botUser, song, musicUrl);
        if (playerMusicList == null) {
            return BotMessage.simpleMusicCloudShareMessage(song.getName(), owner, jumpUrl, pictureUrl, musicUrl);
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", song.getName(), lastStr));
        }
    }

    private BotMessage handleSearch(BotMessageAction messageAction) throws IOException {
        BotRobot bot = messageAction.getBot();
        BotUserDTO botUser = messageAction.getBotUser();
        BotSender botSender = messageAction.getBotSender();
        String searchKey = messageAction.getValue();
        Asserts.notBlank(searchKey, "格式错啦(搜索词)");

        if (searchKey.contains("bilibili.com")) {
            // https://www.bilibili.com/video/BV12L411r7Nh/
            List<String> bvList = StringUtils.pattenAll("(BV\\w{10})", searchKey);
            Asserts.notEmpty(bvList, "啊嘞，不对劲");

            BotMessage resp = null;
            for (String bv : bvList) {
                resp = this.handleBilibiliSearch(bot, botSender, botUser, bv);
            }
            return resp;
        } else if (searchKey.contains("163.com/song")) {
            // https://music.163.com/song?id=446247397&userid=361260659
            List<String> idList = StringUtils.pattenAll("[?&]id=(\\d+)", searchKey);
            BotMessage botMessage = null;
            for (String songId : idList) {
                botMessage = this.handleMusicCouldLink(bot, botSender, botUser, Long.parseLong(songId));
            }
            return botMessage;
        } else if (searchKey.contains("163.com/dj")) {
            // https://music.163.com/dj?id=2071108797&userid=361260659
            List<String> idList = StringUtils.pattenAll("[?&]id=(\\d+)", searchKey);
            BotMessage botMessage = null;
            for (String songId : idList) {
                botMessage = this.handleMusicCouldProgramLink(bot, botSender, botUser, Long.valueOf(songId));
            }
            return botMessage;
        } else {
            return this.handleMusicCouldSearch(bot, botSender, botUser, searchKey);
        }
    }

    private BotMessage handleMusicCouldProgramLink(BotRobot bot, BotSender botSender, BotUserDTO botUser, Long songId) throws IOException {
        MusicCloudProgram program = musicCloudManager.getProgramById(songId);

        String musicUrl = musicCloudManager.getProgramUrlById(program.getMainSong().getId());

        List<PlayerMusic> playerMusicList = musicService.pushVideoToQuote(bot, botSender, botUser, program, musicUrl);
        if (playerMusicList == null) {
            return null;
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", program.getName(), lastStr));
        }
    }

    private BotMessage handleMusicCouldLink(BotRobot bot, BotSender botSender, BotUserDTO botUser, Long songId) throws IOException {
        MusicCloudSong song = musicCloudManager.getSongById(songId);

        String owner = song.getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/"));
        String jumpUrl = "https://y.music.163.com/m/song?id=" + song.getId();
        String pictureUrl = song.getAlbum().getPicUrl();
        String musicUrl = "http://music.163.com/song/media/outer/url?sc=wmv&id=" + song.getId();

        List<PlayerMusic> playerMusicList = musicService.pushVideoToQuote(bot, botSender, botUser, song, musicUrl);
        if (playerMusicList == null) {
            return BotMessage.simpleMusicCloudShareMessage(song.getName(), owner, jumpUrl, pictureUrl, musicUrl);
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", song.getName(), lastStr));
        }
    }

    private BotMessage handleBilibiliSearch(BotRobot bot, BotSender botSender, BotUserDTO botUser, String bv) throws IOException {
        VideoView videoInfo = bilibiliManager.getVideoInfo(bv);
        Long cid = videoInfo.getPages().get(0).getCid();
        String videoUrl = bilibiliManager.getVideoUrl(bv, cid);

        List<PlayerMusic> playerMusicList = musicService.pushVideoToQuote(bot, botSender, botUser, videoInfo, videoUrl);
        if (playerMusicList == null) {
            return BotMessage.simpleVideoMessage(videoInfo.getTitle(), videoUrl);
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", videoInfo.getTitle(), lastStr));
        }
    }

    private BotMessage handleMusicCouldSearch(BotRobot bot, BotSender botSender, BotUserDTO botUser, String searchKey) throws IOException {
        List<MusicCloudSong> songList = musicCloudManager.searchMusicList(searchKey);
        if (songList.size() == 1) {
            MusicCloudSong song = songList.get(0);
            String owner = song.getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/"));
            String jumpUrl = "https://y.music.163.com/m/song?id=" + song.getId();
            String pictureUrl = song.getAlbum().getPicUrl();
            String musicUrl = "http://music.163.com/song/media/outer/url?sc=wmv&id=" + song.getId();

            List<PlayerMusic> playerMusicList = musicService.pushVideoToQuote(bot, botSender, botUser, song, musicUrl);

            List<BotMessageChain> respList = new ArrayList<>();
            respList.add(BotMessageChain.ofPlain(String.format("%s\t\t%s\t\t%s\n", song.getName(), owner, song.getAlbum().getName())));
            if (playerMusicList == null) {
                respList.add(BotMessageChain.ofMusicCloudShare(song.getName(), owner, jumpUrl, pictureUrl, musicUrl));
            } else {
                String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
                return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", song.getName(), lastStr));
            }
            return BotMessage.simpleListMessage(respList);
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
