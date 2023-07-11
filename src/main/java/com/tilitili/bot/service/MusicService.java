package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicSongList;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.bot.BotMessage;
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


    public BotMessage pushPlayListToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, PlayerMusicSongList playerMusicSongList) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        String req = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "musicList", playerMusicSongList));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/add", req);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        List<PlayerMusicDTO> playerMusicList = resp.getData();
        if (playerMusicList == null) {
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

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId(), "music", music));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/add", data);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        List<PlayerMusicDTO> playerMusicList = resp.getData();
        if (playerMusicList == null) {
            return null;
        } else {
            String lastStr = playerMusicList.size() < 2? "": String.format("，前面还有%s首", playerMusicList.size()-1);
            return BotMessage.simpleTextMessage(String.format("点歌[%s]成功%s。", music.getName(), lastStr));
        }
    }

    public List<PlayerMusicDTO> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/last", data);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusicDTO> stopMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/stop", data);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusicDTO> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("textSenderId", botSender.getId(), "voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/start", data);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
        Asserts.notNull(resp, "网络异常");
        Asserts.isTrue(resp.getSuccess(), resp.getMessage());
        return resp.getData();
    }

    public List<PlayerMusicDTO> listMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        String data = Gsons.toJson(ImmutableMap.of("voiceSenderId", voiceSender.getId()));
        String result = HttpClientUtil.httpPost("https://oss.tilitili.club/api/ktv/list", data);
        BaseModel<List<PlayerMusicDTO>> resp = Gsons.fromJson(result, new TypeToken<BaseModel<List<PlayerMusicDTO>>>(){}.getType());
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
