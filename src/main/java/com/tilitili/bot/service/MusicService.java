package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusic;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudProgram;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MusicService {
    private final BotManager botManager;

    public MusicService(BotManager botManager) {
        this.botManager = botManager;
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, VideoView videoView, String musicUrl) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        PlayerMusic music = new PlayerMusic().setFileUrl(musicUrl).setName(videoView.getTitle()).setHeaders("Referer: https://www.bilibili.com");
        return this.reqAddMusics(botSender, voiceSender, music);
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudSong song, String videoUrl) {
        Asserts.notEquals(song.getFee(), 1, "KTV没有VIP喵");
        Asserts.checkNull(song.getNoCopyrightRcmd(), "歌曲下架了喵");

        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        PlayerMusic music = new PlayerMusic().setFileUrl(videoUrl).setName(song.getName());
        return this.reqAddMusics(botSender, voiceSender, music);
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudProgram program, String musicUrl) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        PlayerMusic music = new PlayerMusic().setFileUrl(musicUrl).setName(program.getName());
        return this.reqAddMusics(botSender, voiceSender, music);
    }

    private List<PlayerMusic> reqAddMusics(BotSender botSender, BotSender voiceSender, PlayerMusic music) {
        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "music", music));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/add", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
        return resp.getData();
    }

    public List<PlayerMusic> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/last", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
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
        return resp.getData();
    }

    public List<PlayerMusic> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/start", data);
        BaseModel<List<PlayerMusic>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusic>>>(){}.getType());
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
        return resp.getData();
    }
}
