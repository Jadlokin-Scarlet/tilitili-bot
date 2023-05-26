package com.tilitili.bot.service;

import com.tilitili.bot.util.khl.KhlVoiceConnector;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusic;
import com.tilitili.common.entity.view.bilibili.video.VideoView;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudProgram;
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MusicService {
    private final BotManager botManager;
    private final Map<Long, KhlVoiceConnector> khlVoiceConnectorMap;
    private final SendMessageManager sendMessageManager;

    public MusicService(BotManager botManager, SendMessageManager sendMessageManager) {
        this.sendMessageManager = sendMessageManager;
        this.khlVoiceConnectorMap = new HashMap<>();
        this.botManager = botManager;
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, VideoView videoView, String musicUrl) throws IOException {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("bilibili-video-", ".mp4");
        HttpClientUtil.downloadFile(musicUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",videoView.getBvid());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",videoView.getBvid());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(videoView.getTitle()), botSender);
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudSong song, String videoUrl) throws IOException {
        Asserts.notEquals(song.getFee(), 1, "KTV没有VIP喵");
        Asserts.checkNull(song.getNoCopyrightRcmd(), "歌曲下架了喵");

        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("music-cloud-", ".mp3");
        HttpClientUtil.downloadFile(videoUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",song.getName());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",song.getName());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(song.getName()), botSender);
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudProgram program, String musicUrl) throws IOException {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("music-cloud-", ".mp3");
        HttpClientUtil.downloadFile(musicUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",program.getName());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",program.getName());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(program.getName()), botSender);
    }

    public List<PlayerMusic> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));
        return khlVoiceConnector.lastMusic();
    }

    public List<PlayerMusic> stopMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));
        return khlVoiceConnector.stopMusic();
    }

    public List<PlayerMusic> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));
        return khlVoiceConnector.startMusic();
    }

    public List<PlayerMusic> listMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));
        return khlVoiceConnector.listMusic();
    }

    public Boolean loopPlayer(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot, sendMessageManager));
        return khlVoiceConnector.loopPlayer();
    }
}
