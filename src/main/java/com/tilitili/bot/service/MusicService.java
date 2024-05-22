package com.tilitili.bot.service;

import com.tilitili.bot.entity.MusicSearchKeyHandleResult;
import com.tilitili.common.api.KtvServiceInterface;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.query.PlayerMusicQuery;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BilibiliManager;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
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
    private final PlayerMusicMapper playerMusicMapper;
    private final BotSenderCacheManager botSenderCacheManager;
    private final PlayerMusicListMapper playerMusicListMapper;

    public MusicService(BotManager botManager, BilibiliManager bilibiliManager, MusicCloudManager musicCloudManager, PlayerMusicMapper playerMusicMapper, BotSenderCacheManager botSenderCacheManager, PlayerMusicListMapper playerMusicListMapper) {
        this.botManager = botManager;
        this.bilibiliManager = bilibiliManager;
        this.musicCloudManager = musicCloudManager;
        this.playerMusicMapper = playerMusicMapper;
        this.botSenderCacheManager = botSenderCacheManager;
        this.playerMusicListMapper = playerMusicListMapper;
    }

    public void pushPlayListToQuote(BotRobot bot, BotSender textSender, BotUserDTO botUser, PlayerMusicListDTO playerMusicListDTO) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) return;
        pushPlayListToQuote(textSender, voiceSender, playerMusicListDTO);
    }

    public void pushPlayListToQuote(BotSender textSender, BotSender voiceSender, PlayerMusicListDTO playerMusicListDTO) {
        ktvServiceInterface.addMusic(textSender.getId(), voiceSender.getId(), null, playerMusicListDTO);
    }

    public Boolean pushMusicToQuote(BotRobot bot, BotSender textSender, BotUserDTO botUser, PlayerMusic music) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) {
            return false;
        }
        return pushMusicToQuote(textSender, voiceSender, music);
    }

    public Boolean pushMusicToQuote(BotSender textSender, BotSender voiceSender, PlayerMusic music) {
        ktvServiceInterface.addMusic(textSender.getId(), voiceSender.getId(), music, null);
        return true;
    }

    public void lastMusic(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) return;
        lastMusic(textSender, voiceSender);
    }
    public void lastMusic(BotSender textSender, BotSender voiceSender) {
        ktvServiceInterface.lastMusic(textSender.getId(), voiceSender.getId());
    }

    public void stopMusic(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) return;
        stopMusic(textSender, voiceSender);
    }

    public void stopMusic(BotSender textSender, BotSender voiceSender) {
        ktvServiceInterface.stopMusic(textSender.getId(), voiceSender.getId());
    }

    public void clearMusicList(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) return;

        clearMusicList(textSender, voiceSender);
    }

    public void clearMusicList(BotSender textSender, BotSender voiceSender) {
        ktvServiceInterface.clearMusicList(textSender.getId(), voiceSender.getId());
    }

    public void startMusic(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) return;

        startMusic(textSender, voiceSender);
    }

    public void startMusic(BotSender textSender, BotSender voiceSender) {
        ktvServiceInterface.startMusic(textSender.getId(), voiceSender.getId());
    }

    public Boolean loopPlayer(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) {
            return null;
        }

        return loopPlayer(voiceSender);
    }

    public Boolean loopPlayer(BotSender voiceSender) {
        return ktvServiceInterface.loopPlayer(voiceSender.getId());
    }

    public Boolean restartKtv(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) {
            return null;
        }

        return restartKtv(textSender, voiceSender);
    }

    public Boolean restartKtv(BotSender textSender, BotSender voiceSender) {
        return ktvServiceInterface.restartKtv(textSender.getId(), voiceSender.getId());
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
        PlayerMusicListDTO playerMusicListDTO = null;
        List<PlayerMusic> playerMusicList = null;

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
            playerMusicListDTO = musicCloudManager.getProgramPlayerList(listId);
        } else if (!Objects.equals(StringUtils.patten("163.com/(#/)?(my/)?(m/)?(music/)?playlist", searchKey), "")) {
            // https://music.163.com/playlist?id=649428962&userid=361260659
            Long listId = Long.valueOf(StringUtils.patten1("[?&]id=(\\d+)", searchKey));
            playerMusicListDTO = musicCloudManager.getPlayerList(listId);

            List<String> idList = playerMusicListDTO.getMusicList().stream().map(PlayerMusic::getExternalId).collect(Collectors.toList());
            List<PlayerMusic> playerMusicListDetail = musicCloudManager.getPlayerListDetail(idList);
            playerMusicListDTO.setMusicList(playerMusicListDetail);
        } else if (searchKey.contains("space.bilibili.com")) {
            // https://space.bilibili.com/23210308/favlist?fid=940681308&ftype=create
            String fid = StringUtils.patten1("fid=(\\d+)", searchKey);
            Asserts.notBlank(fid, "啊嘞，不对劲");

            playerMusicListDTO = bilibiliManager.getFavoriteList(fid);
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

                PlayerMusic playerMusic = new PlayerMusic();
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
        return new MusicSearchKeyHandleResult().setPlayerMusicList(playerMusicList).setPlayerMusicListDTO(playerMusicListDTO);
    }

    public PlayerMusicListDTO getMusicListByListId(Integer type, String listId) {
        switch (type) {
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD_PROGRAM:
                return musicCloudManager.getProgramPlayerList(Long.valueOf(listId));
            case PlayerMusicDTO.TYPE_MUSIC_CLOUD:
                PlayerMusicListDTO playerMusicListDTO = musicCloudManager.getPlayerList(Long.valueOf(listId));
                List<String> idList = playerMusicListDTO.getMusicList().stream().map(PlayerMusic::getExternalId).collect(Collectors.toList());
                List<PlayerMusic> playerMusicListDetail = musicCloudManager.getPlayerListDetail(idList);
                playerMusicListDTO.setMusicList(playerMusicListDetail);
                return playerMusicListDTO;
            case PlayerMusicDTO.TYPE_BILIBILI:
                return bilibiliManager.getFavoriteList(listId);
            default:
                throw new AssertException();
        }
    }

    private BotSender getVoiceSenderOrNull(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender;
        try {
            voiceSender = botManager.getUserWhereVoice(bot, textSender, botUser);
        } catch (AssertException e) {
            voiceSender = null;
        }
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }
        return voiceSender;
    }

    public BotSender getTextSenderOrNull(BotSender voiceSender) {
        BotSender textSender;
        try {
            Asserts.notNull(voiceSender.getKookGuildId());
            List<BotSender> senderList = botSenderCacheManager.getBotSenderByCondition(new BotSenderQuery().setKookGuildId(voiceSender.getKookGuildId()).setName("点歌台"));
            Asserts.checkEquals(senderList.size(), 1);
            textSender = senderList.get(0);
        } catch (AssertException e) {
            throw new AssertException("未找到点歌台频道");
        }
        return textSender;
    }

    public void startList(BotSender textSender, BotSender voiceSender, Long listId) {
        PlayerMusicList playerMusicList = playerMusicListMapper.getPlayerMusicListById(listId);
        Asserts.notNull(playerMusicList, "没找到歌单");
        List<PlayerMusic> musicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setListId(listId));
        Asserts.notEmpty(musicList, "歌单空空如也");
        PlayerMusicListDTO playerMusicListDTO = new PlayerMusicListDTO().setMusicList(musicList);
        playerMusicListDTO.setName(playerMusicList.getName());
        this.pushPlayListToQuote(textSender, voiceSender, playerMusicListDTO);
    }

    public void startList(BotRobot bot, BotSender textSender, BotUserDTO botUser) {
        BotSender voiceSender = getVoiceSenderOrNull(bot, textSender, botUser);
        if (voiceSender == null) {
            return;
        }
        startList(textSender, voiceSender, botUser);
    }

    public void startList(BotSender textSender, BotSender voiceSender, BotUserDTO botUser) {
        List<PlayerMusic> musicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(botUser.getId()));
        Asserts.notEmpty(musicList, "歌单空空如也，先导入歌单吧");
        PlayerMusicListDTO playerMusicListDTO = new PlayerMusicListDTO().setMusicList(musicList);
        playerMusicListDTO.setName(botUser.getName() + "的个人歌单");
        this.pushPlayListToQuote(textSender, voiceSender, playerMusicListDTO);
    }

    public void syncMusic(Long userId) {
        List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));
        Asserts.notEmpty(listList, "歌单空空如也，先导入歌单吧");
        for (PlayerMusicList list : listList) {
            PlayerMusicListDTO playerMusicListDTO = this.getMusicListByListId(list.getType(), list.getExternalId());

            PlayerMusicList oldPlayerMusicList = playerMusicListMapper.getPlayerMusicListByUserIdAndTypeAndExternalId(userId, playerMusicListDTO.getType(), playerMusicListDTO.getExternalId());
            if (playerMusicListDTO.getIcon() != null && !playerMusicListDTO.getIcon().equals(oldPlayerMusicList.getIcon())) {
                playerMusicListMapper.updatePlayerMusicListSelective(new PlayerMusicList().setId(oldPlayerMusicList.getId()).setIcon(playerMusicListDTO.getIcon()));
            }

            List<PlayerMusic> newMusicList = playerMusicListDTO.getMusicList();
            List<PlayerMusic> oldMusicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId).setListId(list.getId()));

            List<String> oldExternalIdList = oldMusicList.stream().map(PlayerMusic::getExternalId).collect(Collectors.toList());
            for (PlayerMusic newMusic : newMusicList) {
                if (oldExternalIdList.contains(newMusic.getExternalId())) {
                    continue;
                }
                PlayerMusic otherMusic = playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(userId, newMusic.getType(), newMusic.getExternalId());
                if (otherMusic != null) {
                    playerMusicMapper.updatePlayerMusicSelective(new PlayerMusic().setId(otherMusic.getId()).setListId(list.getId()));
                } else {
                    newMusic.setUserId(userId);
                    newMusic.setListId(list.getId());
                    playerMusicMapper.addPlayerMusicSelective(newMusic);
                }
            }

            List<String> newExternalIdList = newMusicList.stream().map(PlayerMusic::getExternalId).collect(Collectors.toList());
            for (PlayerMusic oldMusic : oldMusicList) {
                if (newExternalIdList.contains(oldMusic.getExternalId())) {
                    continue;
                }
                playerMusicMapper.deletePlayerMusicByPrimary(oldMusic.getId());
            }
        }
    }
}
