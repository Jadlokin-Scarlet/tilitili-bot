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

    public MusicService(BotManager botManager) {
        this.khlVoiceConnectorMap = new HashMap<>();
        this.botManager = botManager;
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, VideoView videoView, String musicUrl) throws IOException {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("bilibili-video-", ".mp4");
        HttpClientUtil.downloadFile(musicUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",videoView.getBvid());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",videoView.getBvid());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(videoView.getTitle()));
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudSong song, String videoUrl) throws IOException {
        Asserts.notEquals(song.getFee(), 1, "KTV没有VIP喵");
        Asserts.checkNull(song.getNoCopyrightRcmd(), "歌曲下架了喵");

        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("music-cloud-", ".mp3");
        HttpClientUtil.downloadFile(videoUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",song.getName());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",song.getName());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(song.getName()));
    }

    public List<PlayerMusic> pushVideoToQuote(BotRobot bot, BotSender botSender, BotUserDTO botUser, MusicCloudProgram program, String musicUrl) throws IOException {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));

        String token = bot.getVerifyKey();
        Asserts.notNull(token, "啊嘞，不对劲");

        File file = File.createTempFile("music-cloud-", ".mp3");
        HttpClientUtil.downloadFile(musicUrl, file);
        Asserts.isTrue(file.exists(), "啊嘞，下载失败了(%s)",program.getName());
        Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了(%s)",program.getName());
        return khlVoiceConnector.pushFileToQueue(token, voiceSender.getKookChannelId(), new PlayerMusic().setFile(file).setName(program.getName()));
    }

    public List<PlayerMusic> lastMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));
        return khlVoiceConnector.lastMusic();
    }

    public List<PlayerMusic> stopMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));
        return khlVoiceConnector.stopMusic();
    }

    public List<PlayerMusic> startMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));
        return khlVoiceConnector.startMusic();
    }

    public List<PlayerMusic> listMusic(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));
        return khlVoiceConnector.listMusic();
    }

    public Boolean loopPlayer(BotRobot bot, BotSender botSender, BotUserDTO botUser) {
        BotSender voiceSender = botManager.getUserWhereVoice(bot, botSender, botUser);
        if (voiceSender == null) {
            log.info("未在语音频道");
            return null;
        }

        KhlVoiceConnector khlVoiceConnector = khlVoiceConnectorMap.computeIfAbsent(voiceSender.getBot(), key -> new KhlVoiceConnector(bot));
        return khlVoiceConnector.loopPlayer();
    }


//    @Async
//    public void asyncPushVideoAsRTSP(BotSender botSender, BotUserDTO botUser, VideoView videoView, String videoUrl) {
//        File file = null;
//        try {
//            BotSender voiceSender = botManager.getUserWhereVoice(botSender, botUser);
//            if (voiceSender == null) {
//                log.info("未在语音频道");
//                return;
//            }
//
//            this.checkPlayerProcess(voiceSender);
//            Long playerChannelId = playerChannelIdMap.get(botSender.getBot());
//            Asserts.notNull(playerChannelId, "启动播放器失败");
//
//            file = File.createTempFile("bilibili-video-", ".mp4");
//            HttpClientUtil.downloadFile(videoUrl, file);
//
//            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
//            if(musicProcess != null){
//                musicProcess.destroy();
//                log.info(">>>>>>>>>>推流视频切换<<<<<<<<<<");
//            }
//            // cmd命令拼接，注意命令中存在空格
////            String command = "ffmpeg -re"; // ffmpeg开头，-re代表按照帧率发送，在推流时必须有
////            command += " -i \"" + file.getPath() + "\" "; // 指定要推送的视频
////            command += " -vn -f flv " + rtmpUrl; // 指定推送服务器，-f：指定格式
//            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -vn -f flv rtp://124.222.94.228:42042?rtcpport=36767
//            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -nostats -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:1234
//            String command = String.format("ffmpeg -re -nostats -i %s -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:5555", file.getPath());
//            log.info("ffmpeg推流命令：" + command);
//
//            // 运行cmd命令，获取其进程
//            musicProcess = Runtime.getRuntime().exec(command);
//            // 输出ffmpeg推流日志
//            BufferedReader br= new BufferedReader(new InputStreamReader(musicProcess.getErrorStream()));
//            String line;
//            while ((line = br.readLine()) != null) {
//                log.info("视频推流信息[" + line + "]");
//            }
//            log.info("视频推流结果"+ musicProcess.waitFor());
//        } catch (AssertException e) {
//            log.warn("推流失败", e);
//            sendMessageManager.sendMessage(BotMessage.simpleTextMessage(e.getMessage()).setBotSender(botSender));
//        } catch (Exception e) {
//            log.warn("推流失败", e);
//        } finally {
//            FileUtil.deleteIfExists(file);
//        }
//    }
//
//    @Async
//    public void asyncPushVideoAsRTSP(BotSender botSender, BotUserDTO botUser, MusicCloudSong song, String musicUrl) {
//        File file = null;
//        try {
//            Asserts.notEquals(song.getFee(), 1, "KTV没有VIP喵");
//            Asserts.checkNull(song.getNoCopyrightRcmd(), "歌曲下架了喵");
//
//            BotSender voiceSender = botManager.getUserWhereVoice(botSender, botUser);
//            if (voiceSender == null) {
//                log.info("未在语音频道");
//                return;
//            }
//
//            this.checkPlayerProcess(voiceSender);
//            Long playerChannelId = playerChannelIdMap.get(botSender.getBot());
//            Asserts.notNull(playerChannelId, "启动播放器失败");
//
//            file = File.createTempFile("music-cloud-", ".mp3");
//            HttpClientUtil.downloadFile(musicUrl, file);
//
//            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
//            if(musicProcess != null){
//                musicProcess.destroy();
//                log.info(">>>>>>>>>>推流视频切换<<<<<<<<<<");
//            }
//            // cmd命令拼接，注意命令中存在空格
////            String command = "ffmpeg -re"; // ffmpeg开头，-re代表按照帧率发送，在推流时必须有
////            command += " -i \"" + file.getPath() + "\" "; // 指定要推送的视频
////            command += " -vn -f flv " + rtmpUrl; // 指定推送服务器，-f：指定格式
//            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -vn -f flv rtp://124.222.94.228:42042?rtcpport=36767
//            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -nostats -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:1234
//            String command = String.format("ffmpeg -re -nostats -i %s -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:5555", file.getPath());
//            log.info("ffmpeg推流命令：" + command);
//
//            // 运行cmd命令，获取其进程
//            musicProcess = Runtime.getRuntime().exec(command);
//            // 输出ffmpeg推流日志
//            BufferedReader br= new BufferedReader(new InputStreamReader(musicProcess.getErrorStream()));
//            String line;
//            while ((line = br.readLine()) != null) {
//                log.info("视频推流信息[" + line + "]");
//            }
//            log.info("视频推流结果"+ musicProcess.waitFor());
//        } catch (AssertException e) {
//            log.warn("推流失败", e);
//            sendMessageManager.sendMessage(BotMessage.simpleTextMessage(e.getMessage()).setBotSender(botSender));
//        } catch (Exception e) {
//            log.warn("推流失败", e);
//        } finally {
//            FileUtil.deleteIfExists(file);
//        }
//    }
//    private void checkPlayerProcess(BotSender voiceSender) throws ExecutionException, InterruptedException, IOException {
//        Long channelId = voiceSender.getKookChannelId();
//        BotEnum bot = BotEnum.getBotById(voiceSender.getBot());
//        Asserts.notNull(bot, "啊嘞，不对劲");
//
//        Long playerChannelId = playerChannelIdMap.get(voiceSender.getBot());
//        if(Objects.equals(playerChannelId, channelId)) {
//            log.info("无需切换播放器");
//            return;
//        }
//
//        if (playerChannelId != null) {
//            log.info("重启播放器");
//            khlVoiceConnector.disconnect();
//        }
//
//        playerChannelIdMap.put(voiceSender.getBot(), channelId);
//        String rtmpUrl = khlVoiceConnector.connect(voiceSender, () -> {
//            playerChannelIdMap.remove(voiceSender.getBot());
//            return null;
//        }).get();
//
//
//        String command = String.format("ffmpeg -re -loglevel level+info -nostats -stream_loop -1 -i zmq:tcp://127.0.0.1:5555 -map 0:a:0 -acodec libopus -ab 128k -filter:a volume=0.8 -ac 2 -ar 48000 -f tee [select=a:f=rtp:ssrc=1357:payload_type=100]%s", rtmpUrl);
//        log.info("ffmpeg开启推流命令：" + command);
//        Process playerProcess = Runtime.getRuntime().exec(command);
//        TimeUtil.millisecondsSleep(1000);
//    }
}
