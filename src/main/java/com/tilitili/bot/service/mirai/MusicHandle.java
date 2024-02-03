package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.MusicSearchKeyHandleResult;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.MusicType;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicSongList;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.query.PlayerMusicQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudOwner;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.MusicCloudManager;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MusicHandle extends ExceptionRespMessageHandle {
    private final RedisCache redisCache;
    private final MusicService musicService;
    private final MusicCloudManager musicCloudManager;
    private final AtomicBoolean lockFlag = new AtomicBoolean(false);
    private final PlayerMusicMapper playerMusicMapper;
    private final PlayerMusicListMapper playerMusicListMapper;

    public MusicHandle(RedisCache redisCache, MusicCloudManager musicCloudManager, MusicService musicService, PlayerMusicMapper playerMusicMapper, PlayerMusicListMapper playerMusicListMapper) {
        this.redisCache = redisCache;
        this.musicService = musicService;
        this.musicCloudManager = musicCloudManager;
        this.playerMusicMapper = playerMusicMapper;
        this.playerMusicListMapper = playerMusicListMapper;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        try {
            Asserts.isTrue(lockFlag.compareAndSet(false, true), "猪脑过载，你先别急 Σ（ﾟдﾟlll）");
            switch (messageAction.getKeyWithoutPrefix()) {
                case "选歌": return handleChoose(messageAction);
                case "点歌": case "dg": return handleSearch(messageAction, null);
                case "点歌1": return handleSearch(messageAction, 0);
                case "切歌": return handleLast(messageAction);
    //            case "绑定KTV": return handleBindKTV(messageAction);
                case "暂停": return handleStop(messageAction);
                case "继续": return handleStart(messageAction);
                case "清空": return handleClear(messageAction);
                case "播放列表": return handleList(messageAction);
                case "循环播放": return handleLoopPlayer(messageAction);
                case "停止": return handleRestartKtv(messageAction);
                case "歌单": return handleSongList(messageAction);
                default: throw new AssertException();
            }
        } finally {
            lockFlag.set(false);
        }
    }

    private BotMessage handleSongList(BotMessageAction messageAction) {
        if ("是".equals(messageAction.getValue())) {
            return handleConfirmClear(messageAction);
        }
        switch (messageAction.getSubKey()) {
            case "导入": return handleSongListImport(messageAction);
            case "删除": return handleDeleteTheMusic(messageAction);
            case "清空": return handleClearSongList(messageAction);
            case "播放": case "启动": return handlePlaySongList(messageAction);
            case "同步": return handleSyncList(messageAction);
            case "": return handleShowList(messageAction);
            default: throw new AssertException();
        }
    }

    private BotMessage handleShowList(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        List<PlayerMusicList> musicListList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));

        List<String> textList = IntStream.range(0, musicListList.size())
                .mapToObj(index -> String.format("%s：%s(%s)", index, musicListList.get(index).getName(), MusicType.getByValue(musicListList.get(index).getType()).getText()))
                .collect(Collectors.toList());
        textList.add(0, String.format("%s的个人歌单：", botUser.getName()));

        return BotMessage.simpleListMessage(Collections.singletonList(
                new BotMessageChain().setType(BotMessage.MESSAGE_TYPE_CARD_MUSIC_LIST).setTextList(textList)
        ));
    }

    private BotMessage handleSyncList(BotMessageAction messageAction) {
        Long userId = messageAction.getBotUser().getId();
        List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));
        Asserts.notEmpty(listList, "歌单空空如也，先导入歌单吧");
        for (PlayerMusicList list : listList) {
            PlayerMusicSongList playerMusicSongList = musicService.getMusicListByListId(list.getType(), list.getExternalId());
            List<PlayerMusicDTO> newMusicList = playerMusicSongList.getMusicList();
            List<PlayerMusic> oldMusicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId).setListId(list.getId()));

            List<String> oldExternalIdList = oldMusicList.stream().map(PlayerMusic::getExternalId).collect(Collectors.toList());
            for (PlayerMusicDTO newMusic : newMusicList) {
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
        return BotMessage.simpleTextMessage("同步完毕");
    }

    private BotMessage handlePlaySongList(BotMessageAction messageAction) {
        BotRobot bot = messageAction.getBot();
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        List<PlayerMusicDTO> musicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(botUser.getId())).stream().map(music -> {
            PlayerMusicDTO playerMusic = new PlayerMusicDTO();
            playerMusic.setType(music.getType()).setExternalId(music.getExternalId()).setExternalSubId(music.getExternalSubId()).setName(music.getName()).setIcon(music.getIcon());
            return playerMusic;
        }).collect(Collectors.toList());
        Asserts.notEmpty(musicList, "歌单空空如也，先导入歌单吧");
        PlayerMusicSongList playerMusicSongList = new PlayerMusicSongList().setName(botUser.getName() + "的个人歌单").setMusicList(musicList);
        return musicService.pushPlayListToQuote(bot, botSender, botUser, playerMusicSongList);
    }

    private BotMessage handleConfirmClear(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        if (!redisCache.exists(String.format("MusicHandle.clearSongListConfirm-%s-%s", botSender.getId(), botUser.getId()))) {
            return null;
        }
        List<PlayerMusicList> musicListList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(botUser.getId()));
        List<PlayerMusic> musicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(botUser.getId()));
        Asserts.isFalse(musicList.isEmpty() && musicListList.isEmpty(), "歌单空空如也");
        for (PlayerMusic music : musicList) {
            playerMusicMapper.deletePlayerMusicByPrimary(music.getId());
        }
        for (PlayerMusicList list : musicListList) {
            playerMusicListMapper.deletePlayerMusicListByPrimary(list.getId());
        }
        return BotMessage.simpleTextMessage("好了喵("+musicList.size()+")");
    }

    private BotMessage handleClearSongList(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        redisCache.setValue(String.format("MusicHandle.clearSongListConfirm-%s-%s", botSender.getId(), botUser.getId()), "yes", 60);
        return BotMessage.simpleAtTextMessage("确定清空个人歌单吗(是/否)", botUser);
    }

    private BotMessage handleDeleteTheMusic(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        String searchKey = messageAction.getSubValue();
        if (StringUtils.isNotBlank(searchKey)) {
            MusicSearchKeyHandleResult musicSearchKeyHandleResult = musicService.handleSearchKey(searchKey);
            List<PlayerMusicDTO> playerMusicList = musicSearchKeyHandleResult.getPlayerMusicList();
            PlayerMusicSongList playerMusicSongList = musicSearchKeyHandleResult.getPlayerMusicSongList();
            if (playerMusicList == null) {
                playerMusicList = new ArrayList<>();
            }
            int musicCnt = 0;
            int listCnt = 0;
            if (playerMusicSongList != null) {
                playerMusicList.addAll(playerMusicSongList.getMusicList());
                PlayerMusicList dbList = playerMusicListMapper.getPlayerMusicListByUserIdAndTypeAndExternalId(userId, playerMusicSongList.getType(), playerMusicSongList.getExternalId());
                if (dbList != null) {
                    playerMusicListMapper.deletePlayerMusicListByPrimary(dbList.getId());
                    listCnt++;
                }
            }
            for (PlayerMusicDTO playerMusic : playerMusicList) {
                PlayerMusic dbMusic = playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(userId, playerMusic.getType(), playerMusic.getExternalId());
                if (dbMusic != null) {
                    playerMusicMapper.deletePlayerMusicByPrimary(dbMusic.getId());
                    musicCnt++;
                }
            }
            return BotMessage.simpleTextMessage(String.format("删除歌单%d歌曲%d成功.", listCnt, musicCnt));
        } else {
            PlayerMusicDTO theMusic = musicService.getTheMusic(messageAction.getBot(), messageAction.getBotSender(), botUser);
            if (theMusic == null) {
                return null;
            }
            PlayerMusic dbPlayerMusic = playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(botUser.getId(), theMusic.getType(), theMusic.getExternalId());
            Asserts.notNull(dbPlayerMusic, "当前歌曲不在你的歌单中");
            playerMusicMapper.deletePlayerMusicByPrimary(dbPlayerMusic.getId());
            return BotMessage.simpleTextMessage("删除成功，歌曲地址：" + musicService.getMusicJumpUrl(theMusic));
        }
    }

    private BotMessage handleSongListImport(BotMessageAction messageAction) {
        BotUserDTO botUser = messageAction.getBotUser();
        Long userId = botUser.getId();
        String searchKey = messageAction.getSubValue();
        if (StringUtils.isBlank(searchKey)) {
            messageAction.getSession().put("waitSearchKeyList"+ userId, "yes", 60 * 60 * 3);
            return BotMessage.simpleAtTextMessage("请输入以下之一：①网易云歌单链接②b站收藏夹链接", botUser);
        }
        MusicSearchKeyHandleResult musicSearchKeyHandleResult = musicService.handleSearchKey(searchKey);
        List<PlayerMusicDTO> playerMusicList = musicSearchKeyHandleResult.getPlayerMusicList();
        PlayerMusicSongList playerMusicSongList = musicSearchKeyHandleResult.getPlayerMusicSongList();
        if (playerMusicList == null) {
            playerMusicList = new ArrayList<>();
        }
        Long listId = null;
        if (playerMusicSongList != null) {
            playerMusicList.addAll(playerMusicSongList.getMusicList());
            if (playerMusicListMapper.getPlayerMusicListByUserIdAndTypeAndExternalId(userId, playerMusicSongList.getType(), playerMusicSongList.getExternalId()) == null) {
                PlayerMusicList newList = new PlayerMusicList().setUserId(userId).setName(playerMusicSongList.getName()).setType(playerMusicSongList.getType()).setExternalId(playerMusicSongList.getExternalId());
                playerMusicListMapper.addPlayerMusicListSelective(newList);
                listId = newList.getId();
            }
        }
        for (PlayerMusicDTO playerMusic : playerMusicList) {
            playerMusic.setUserId(userId);
            playerMusic.setListId(listId);
            if (playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(userId, playerMusic.getType(), playerMusic.getExternalId()) == null) {
                playerMusicMapper.addPlayerMusicSelective(playerMusic);
            }
        }

        return BotMessage.simpleTextMessage("导入成功("+playerMusicList.size()+")");
    }

    private BotMessage handleRestartKtv(BotMessageAction messageAction) {
        Boolean success = musicService.restartKtv(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (success == null) {
            return null;
        }
        if (success) {
            return BotMessage.simpleTextMessage("已停止。");
        } else {
            throw new AssertException();
        }
    }

    private BotMessage handleLoopPlayer(BotMessageAction messageAction) {
        Boolean success = musicService.loopPlayer(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (success == null) {
            return null;
        }
        if (success) {
            return BotMessage.simpleTextMessage("(仅)当前歌曲将会循环播放，发送切歌播放下一首。");
        } else {
            throw new AssertException();
        }
    }

    private BotMessage handleList(BotMessageAction messageAction) {
        List<String> playerMusicList = musicService.listMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.isEmpty()) {
            return BotMessage.simpleTextMessage("播放列表空空如也。");
        } else {
            List<String> respList = new ArrayList<>();
            for (int index = 0; index < playerMusicList.size(); index++) {
                String playerMusic = playerMusicList.get(index);
                String indexStr = index == 0? "当前": index == 1? "下一首": String.valueOf(index);
                respList.add(String.format("%s：%s", indexStr, playerMusic));
            }
            return BotMessage.simpleTextMessage(String.join("\n", respList));
        }
    }

    private BotMessage handleStart(BotMessageAction messageAction) {
        List<String> playerMusicList = musicService.startMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
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
        List<String> playerMusicList = musicService.stopMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.size() < 2) {
            return BotMessage.simpleTextMessage("已暂停，无下一首，点歌以继续。");
        }
        return BotMessage.simpleTextMessage(String.format("已暂停，输入继续播放下一首：[%s]。", playerMusicList.get(1)));
    }

    private BotMessage handleClear(BotMessageAction messageAction) {
        List<String> playerMusicList = musicService.clearMusicList(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        return BotMessage.simpleTextMessage("已清理播放列表，无下一首，点歌以继续。");
    }

    private BotMessage handleLast(BotMessageAction messageAction) {
        List<String> playerMusicList = musicService.lastMusic(messageAction.getBot(), messageAction.getBotSender(), messageAction.getBotUser());
        if (playerMusicList == null) {
            return null;
        }
        if (playerMusicList.size() < 2) {
            return BotMessage.simpleTextMessage("播放列表空空如也。");
        }
        return BotMessage.emptyMessage();
    }

    private BotMessage handleChoose(BotMessageAction messageAction) {
        BotRobot bot = messageAction.getBot();
        BotUserDTO botUser = messageAction.getBotUser();
        BotSender botSender = messageAction.getBotSender();
        String redisKey = "songList-" + botUser.getId();
        if (!redisCache.exists(redisKey)) {
            return null;
        }
        String value = messageAction.getValue();
        Asserts.isNumber(value, "格式错啦(序号)");
        int index = Integer.parseInt(value) - 1;

        List<MusicCloudSong> songList = (List<MusicCloudSong>) redisCache.getValue(redisKey);
        MusicCloudSong song = songList.get(index);
        redisCache.delete(redisKey);

        return handleMusicCouldLink(bot, botSender, botUser, song);
    }

    private BotMessage handleSearch(BotMessageAction messageAction, Integer index) {
        BotUserDTO botUser = messageAction.getBotUser();
        BotRobot bot = messageAction.getBot();
        BotSender botSender = messageAction.getBotSender();
        String searchKey = messageAction.getValue();
        if (StringUtils.isBlank(searchKey)) {
            messageAction.getSession().put("waitSearchKey-"+messageAction.getBotUser().getId(), "yes", 60 * 60 * 3);
            return BotMessage.simpleAtTextMessage("请输入以下之一：①网易云歌曲/歌单链接②b站视频/收藏夹链接③网易云歌曲名", botUser);
        }
        Asserts.notBlank(searchKey, "格式错啦(搜索词)");

        MusicSearchKeyHandleResult musicSearchKeyHandleResult = musicService.handleSearchKey(searchKey);
        List<PlayerMusicDTO> playerMusicList = musicSearchKeyHandleResult.getPlayerMusicList();
        PlayerMusicSongList playerMusicSongList = musicSearchKeyHandleResult.getPlayerMusicSongList();
        if (playerMusicList != null) {
            BotMessage botMessage = null;
            for (PlayerMusicDTO playerMusic : playerMusicList) {
                botMessage = musicService.pushMusicToQuote(bot, botSender, botUser, playerMusic);
            }
            return botMessage;
        } else if (playerMusicSongList != null) {
            return musicService.pushPlayListToQuote(bot, botSender, botUser, playerMusicSongList);
        } else {
            return this.handleMusicCouldSearch(bot, botSender, botUser, searchKey, index);
        }
    }

    private BotMessage handleMusicCouldLink(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudSong song) {
        Integer type = song.getFee() == 1? PlayerMusicDTO.TYPE_MUSIC_CLOUD_VIP: PlayerMusicDTO.TYPE_MUSIC_CLOUD;
        PlayerMusicDTO playerMusic = new PlayerMusicDTO();
        playerMusic.setType(type).setName(song.getName()).setExternalId(String.valueOf(song.getId())).setIcon(song.getAlbum().getPicUrl());
        return musicService.pushMusicToQuote(bot, botSender, botUser, playerMusic);
    }

    private BotMessage handleMusicCouldSearch(BotRobot bot, BotSender botSender, BotUserDTO botUser, String searchKey, Integer theIndex) {
        List<MusicCloudSong> songList = musicCloudManager.searchMusicList(bot, searchKey);
        Asserts.notEmpty(songList, "没搜到歌曲");
        if (songList.size() == 1) {
            MusicCloudSong song = songList.get(0);
            return handleMusicCouldLink(bot, botSender, botUser, song);
        } else if (theIndex != null) {
            MusicCloudSong song = songList.get(theIndex);
            return handleMusicCouldLink(bot, botSender, botUser, song);
        } else {
            String resp = IntStream.range(0, songList.size()).mapToObj(index -> String.format("%s:%s%s\t%s\t%s",
                    index + 1,
                    songList.get(index).getName(),
                    songList.get(index).getFee() == 1? "（VIP）": songList.get(index).getNoCopyrightRcmd() != null? "（下架）": "",
                    songList.get(index).getOwnerList().stream().map(MusicCloudOwner::getName).collect(Collectors.joining("/")),
                    songList.get(index).getAlbum().getName()
            )).collect(Collectors.joining("\n"));
            redisCache.setValue("songList-" + botUser.getId(), songList, 60);
            return BotMessage.simpleListMessage(Lists.newArrayList(
                    BotMessageChain.ofAt(botUser),
                    BotMessageChain.ofPlain("搜索结果如下，输入序号选择歌曲\n" + resp)
            ));
        }
    }

    @Override
    public String isThisTask(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        if (redisCache.exists("songList-"+botUser.getId()) && StringUtils.isNumber(messageAction.getText())) {
            return "选歌";
        }
        if (redisCache.exists(String.format("MusicHandle.clearSongListConfirm-%s-%s", botSender.getId(), botUser.getId()))) {
            return "歌单";
        }
        if (messageAction.getSession().containsKey("waitSearchKey"+messageAction.getBotUser().getId())) {
            return "点歌";
        }
        if (messageAction.getSession().containsKey("waitSearchKeyList"+messageAction.getBotUser().getId())) {
            return "歌单 导入";
        }

        return null;
    }
}
