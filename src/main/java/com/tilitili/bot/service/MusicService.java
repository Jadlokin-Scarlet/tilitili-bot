package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusic;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudPlayerListData;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudTrackId;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MusicService {
    private final BotManager botManager;

    public MusicService(BotManager botManager) {
        this.botManager = botManager;
    }


    public List<PlayerMusic> pushPlayListToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudPlayerListData data) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        List<String> idList = data.getTrackIds().stream().map(MusicCloudTrackId::getId).map(String::valueOf).collect(Collectors.toList());
        PlayerMusic music = new PlayerMusic().setName(data.getName()).setRollPlayer(true).setType(PlayerMusic.TYPE_MUSIC_CLOUD).setIdList(idList);
        String req = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "music", music));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/add", req);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public BotMessage pushMusicToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, PlayerMusic music) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }
        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "music", music));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/add", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        List<PlayerMusic> playerMusicList = resp.getData();
        if (playerMusicList == null) {
            return null;
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", music.getName(), lastStr));
        }
    }

    public List<PlayerMusic> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/last", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusic> stopMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/stop", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusic> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/start", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusic> listMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/list", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public Boolean loopPlayer(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/loop", data);
        BaseModel<Boolean> resp = Gsons.fromJson(result, new TypeToken<BaseModel<Boolean>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }
}
