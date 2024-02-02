package com.tilitili.bot.service;

import com.tilitili.bot.entity.MusicSearchKeyHandleResult;
import com.tilitili.common.api.KtvServiceInterface;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicSongList;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MusicService {
    private final BotManager botManager;
    private final BilibiliManager bilibiliManager;
    private final MusicCloudManager musicCloudManager;
    @DubboReference(timeout = 8000)
    private KtvServiceInterface ktvServiceInterface;

    public MusicService(BotManager botManager, BilibiliManager bilibiliManager, MusicCloudManager musicCloudManager) {
        this.botManager = botManager;
        this.bilibiliManager = bilibiliManager;
        this.musicCloudManager = musicCloudManager;
    }

    public BotMessage pushPlayListToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, PlayerMusicSongList playerMusicSongList) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        List<String> playerMusicNameList = ktvServiceInterface.addMusic(botSender.getId(), voiceSender.getId(), null, playerMusicSongList);
//        String req = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "musicList", playerMusicSongList));
//        String result = this.post("add", req);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        List<String> playerMusicNameList = resp.getData();
        if (playerMusicNameList == null) {
            return null;
        } else {
            return BotMessage.simpleTextMessage(String.format("加载歌单[%s]成功，即将随机播放。", playerMusicSongList.getName()));
        }
    }

    public BotMessage pushMusicToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, PlayerMusicDTO music) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }
        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");


        List<String> playerMusicNameList = ktvServiceInterface.addMusic(botSender.getId(), voiceSender.getId(), music, null);
//        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "music", music));
//        String result = this.post("add", data);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        List<String> playerMusicNameList = resp.getData();
        if (playerMusicNameList == null) {
            return null;
        } else {
            String lastStr = playerMusicNameList.size() < 2? "": String.format("，前面还有%s首", playerMusicNameList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", music.getName(), lastStr));
        }
    }

    public List<String> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.lastMusic(botSender.getId(), voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
//        String result = this.post("last", data);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public List<String> stopMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.stopMusic(voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
//        String result = this.post("stop", data);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public List<String> clearMusicList(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.clearMusicList(voiceSender.getId());
    }

    public List<String> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.startMusic(botSender.getId(), voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
//        String result = this.post("start", data);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public List<String> listMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.listMusic(voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
//        String result = this.post("list", data);
//        BaseModel<List<String>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<String>>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public Boolean loopPlayer(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.loopPlayer(voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
//        String result = this.post("loop", data);
//        BaseModel<Boolean> resp = Gsons.fromJson(result, new TypeToken<BaseModel<Boolean>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public Boolean restartKtv(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.restartKtv(voiceSender.getId());
//        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
//        String result = this.post("restart", data);
//        BaseModel<Boolean> resp = Gsons.fromJson(result, new TypeToken<BaseModel<Boolean>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public PlayerMusicDTO getTheMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        return ktvServiceInterface.getTheMusic(voiceSender.getId());
//        String result = HttpClientUtil.httpGet("https://oss.tilitili.club/api/ktv/get?voiceSenderId="+voiceSender.getId());
//        BaseModel<PlayerMusicDTO> resp = Gsons.fromJson(result, new TypeToken<BaseModel<PlayerMusicDTO>>(){}.getType());
//        Asserts.notNull(resp, "网络异常");
//        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
//        return resp.getData();
    }

    public String getMusicJumpUrl(PlayerMusicDTO theMusic) {
        switch (theMusic.getType()) {
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD:
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD_VIP: return "https://music.163.com/song?id="+theMusic.getExternalId();
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD_PROGRAM: return "https://music.163.com/dj?id="+theMusic.getExternalId();
            case PlayerMusicDTO.TYPE_BILIBILI: return "https://www.bilibili.com/video/"+theMusic.getExternalId();
            default: throw new AssertException();
        }
    }


    public MusicSearchKeyHandleResult handleSearchKey(String searchKey) {
        PlayerMusicSongList playerMusicSongList = null;
        List<PlayerMusicDTO> playerMusicList = null;

        // 短网址
        if (searchKey.contains("b23.tv")) {
            // https://b23.tv/NtQiyU8
            String shortUrl = StringUtils.patten("https://b23.tv/\\w+", searchKey);
            searchKey = HttpClientUtil.resolveShortUrl(shortUrl);
        } else if (searchKey.contains("163cn.tv")) {
            // http://163cn.tv/1DpBt0
            String shortUrl = StringUtils.patten("http://163cn.tv/\\w+", searchKey);
            searchKey = HttpClientUtil.resolveShortUrl(shortUrl);
        }

        List<String> searchBvList = StringUtils.pattenAll("BV\\w{10}", searchKey);

        if (searchKey.contains("163.com/#/djradio") || searchKey.contains("163.com/radio")) {
            Long listId = Long.valueOf(StringUtils.patten1("[?&]id=(\\d+)", searchKey));
            playerMusicSongList = musicCloudManager.getProgramPlayerList(listId);
        } else if (!Objects.equals(StringUtils.patten("163.com/(#/)?(my/)?(m/)?(music/)?playlist", searchKey), "")) {
            // https://music.163.com/playlist?id=649428962&userid=361260659
            Long listId = Long.valueOf(StringUtils.patten1("[?&]id=(\\d+)", searchKey));
            playerMusicSongList = musicCloudManager.getPlayerList(listId);

            List<String> idList = playerMusicSongList.getMusicList().stream().map(PlayerMusicDTO::getExternalId).collect(Collectors.toList());
            List<PlayerMusicDTO> playerMusicListDetail = musicCloudManager.getPlayerListDetail(idList);
            playerMusicSongList.setMusicList(playerMusicListDetail);
        } else if (searchKey.contains("space.bilibili.com")) {
            // https://space.bilibili.com/23210308/favlist?fid=940681308&ftype=create
            String fid = StringUtils.patten1("fid=(\\d+)", searchKey);
            Asserts.notBlank(fid, "啊嘞，不对劲");

            playerMusicSongList = bilibiliManager.getFavoriteList(fid);
        } else if (searchKey.contains("bilibili.com") || !searchBvList.isEmpty()) {
            // https://www.bilibili.com/video/BV12L411r7Nh/
            Asserts.notEmpty(searchBvList, "啊嘞，不对劲");
            String pnStr = searchBvList.size() < 2? StringUtils.patten1("\\?p=(\\d+)", searchKey): null;

            playerMusicList = new ArrayList<>();
            for (String bv : searchBvList) {
                VideoView videoInfo = bilibiliManager.getVideoInfo(bv);
                Asserts.notNull(videoInfo, "获取视频信息失败");
                Asserts.notEmpty(videoInfo.getPages(), "获取视频信息失败");
                int pn = StringUtils.isBlank(pnStr)? 0: Integer.parseInt(pnStr) - 1;

                PlayerMusicDTO playerMusic = new PlayerMusicDTO();
                playerMusic.setType(PlayerMusicDTO.TYPE_BILIBILI).setName(videoInfo.getTitle())
                        .setExternalId(bv).setExternalSubId(String.valueOf(videoInfo.getPages().get(pn).getCid()))
                        .setIcon(videoInfo.getPic());
                playerMusicList.add(playerMusic);
            }
        } else if (!Objects.equals(StringUtils.patten("163.com/(#/)?(my/)?(m/)?(music/)?song", searchKey), "")) {
//        } else if (searchKey.contains("163.com/song") || searchKey.contains("163.com/#/song") || searchKey.contains("163.com/#/program")) {
            // https://music.163.com/song?id=446247397&userid=361260659
            List<String> idList = StringUtils.pattenAll("(?<=[?&]id=)\\d+", searchKey).stream().distinct().collect(Collectors.toList());

            playerMusicList = musicCloudManager.getPlayerListDetail(idList);
        } else if (!Objects.equals(StringUtils.patten("163.com/(#/)?(my/)?(m/)?(music/)?(dj|program)", searchKey), "")) {
            // https://music.163.com/dj?id=2071108797&userid=361260659
            List<String> idList = StringUtils.pattenAll("(?<=[?&]id=)\\d+", searchKey).stream().distinct().collect(Collectors.toList());

            playerMusicList = new ArrayList<>();
            for (String songId : idList) {
                playerMusicList.add(musicCloudManager.getProgramById(songId));
            }
        }
        return new MusicSearchKeyHandleResult().setPlayerMusicList(playerMusicList).setPlayerMusicSongList(playerMusicSongList);
    }

    public PlayerMusicSongList getMusicListByListId(Integer type, String listId) {
        switch (type) {
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD_PROGRAM:
                return musicCloudManager.getProgramPlayerList(Long.valueOf(listId));
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD:
                PlayerMusicSongList playerMusicSongList = musicCloudManager.getPlayerList(Long.valueOf(listId));
                List<String> idList = playerMusicSongList.getMusicList().stream().map(PlayerMusicDTO::getExternalId).collect(Collectors.toList());
                List<PlayerMusicDTO> playerMusicListDetail = musicCloudManager.getPlayerListDetail(idList);
                playerMusicSongList.setMusicList(playerMusicListDetail);
                return playerMusicSongList;
            case PlayerMusicDTO.TYPE_BILIBILI:
                return bilibiliManager.getFavoriteList(listId);
            default:
                throw new AssertException();
        }
    }
}
