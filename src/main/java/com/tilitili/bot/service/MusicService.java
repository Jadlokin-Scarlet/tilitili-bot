package com.tilitili.bot.service;

import com.tilitili.bot.util.khl.KhlVoiceConnector;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class MusicService {
    private Process musicProcess;
    private Long playerChannelId;
    private String rtmpUrl;

    private final BotSenderMapper botSenderMapper;
    private final KhlVoiceConnector khlVoiceConnector;

    public MusicService(BotSenderMapper botSenderMapper, KhlVoiceConnector khlVoiceConnector) {
        this.botSenderMapper = botSenderMapper;
        this.khlVoiceConnector = khlVoiceConnector;
    }

    @Async
    public void asyncPushVideoAsRTSP(Long senderId, String musicUrl) {
        try {
            this.checkPlayerProcess(senderId);
            Asserts.notNull(rtmpUrl, "找不到地址");

            File file = File.createTempFile("music-cloud-", ".mp3");
            HttpClientUtil.downloadFile(musicUrl, file);

            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
            if(musicProcess != null){
                musicProcess.destroy();
                log.info(">>>>>>>>>>推流视频切换<<<<<<<<<<");
            }
            // cmd命令拼接，注意命令中存在空格
//            String command = "ffmpeg -re"; // ffmpeg开头，-re代表按照帧率发送，在推流时必须有
//            command += " -i \"" + file.getPath() + "\" "; // 指定要推送的视频
//            command += " -vn -f flv " + rtmpUrl; // 指定推送服务器，-f：指定格式
            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -vn -f flv rtp://124.222.94.228:42042?rtcpport=36767
            // ffmpeg -re -i "/tmp/music-cloud-4397789076346411063.mp3" -nostats -acodec libopus -ab 128k -f mpegts zmq:tcp://127.0.0.1:1234
            String command = String.format("ffmpeg -re -loglevel level+info -nostats -i \"%s\" -map 0:a:0 -acodec libopus -ab 128k -filter:a volume=0.8 -ac 2 -ar 48000 -f tee [select=a:f=rtp:ssrc=1357:payload_type=100]%s", file.getPath(), rtmpUrl);
            log.info("ffmpeg推流命令：" + command);

            // 运行cmd命令，获取其进程
            musicProcess = Runtime.getRuntime().exec(command);
            // 输出ffmpeg推流日志
            BufferedReader br= new BufferedReader(new InputStreamReader(musicProcess.getErrorStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                log.info("视频推流信息[" + line + "]");
            }
            log.info("结果"+ musicProcess.waitFor());
            Files.deleteIfExists(file.toPath());
        } catch (Exception e) {
            log.warn("推流失败", e);
        }
    }

    public void lastMusic() {
        if(musicProcess != null){
            musicProcess.destroy();
            log.info(">>>>>>>>>>推流视频切换<<<<<<<<<<");
        }
    }
    
    private void checkPlayerProcess(Long senderId) throws ExecutionException, InterruptedException {
        BotSender sourceSender = botSenderMapper.getValidBotSenderById(senderId);
        List<BotSender> otherSenderList = botSenderMapper.getBotSenderByCondition(new BotSenderQuery().setKookGuildId(sourceSender.getKookGuildId()).setStatus(0));
        BotSender ktvSender = otherSenderList.stream().filter(sender -> Objects.equals(sender.getName(), "KTV")).findFirst().orElse(null);
        Asserts.notNull(ktvSender, "无KTV");
        Long channelId = ktvSender.getKookChannelId();

        if(Objects.equals(this.playerChannelId, channelId)) {
            log.info("无需切换播放器");
            return;
        }

        if (playerChannelId != null) {
            log.info("重启播放器");
            khlVoiceConnector.disconnect();
        }

        this.playerChannelId = channelId;
        rtmpUrl = khlVoiceConnector.connect(channelId, () -> {
            playerChannelId = null;
            return null;
        }).get();
    }
}
