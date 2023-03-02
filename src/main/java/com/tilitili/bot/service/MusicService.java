package com.tilitili.bot.service;

import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MusicService {
    private Process musicProcess;
    private Process playerProcess;
    private Long playerChannelId;

    private final BotSenderMapper botSenderMapper;

    public MusicService(BotSenderMapper botSenderMapper) {
        this.botSenderMapper = botSenderMapper;
    }

    @Async
    public void asyncPushVideoAsRTSP(Long senderId, String musicUrl) {
        try {
            this.checkPlayerProcess(senderId);

            File file = File.createTempFile("music-cloud-", ".mp3");
            HttpClientUtil.downloadFile(musicUrl, file);

            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
            if(musicProcess != null){
                musicProcess.destroy();
                log.info(">>>>>>>>>>推流视频切换<<<<<<<<<<");
            }
            // cmd命令拼接，注意命令中存在空格
            String command = "ffmpeg -re"; // ffmpeg开头，-re代表按照帧率发送，在推流时必须有
            command += " -i " + file.getPath(); // 指定要推送的视频
            command += " -vn -f flv rtmp://121.5.247.29/live/livestream"; // 指定推送服务器，-f：指定格式
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
    
    private void checkPlayerProcess(Long senderId) throws IOException {
        BotSender sourceSender = botSenderMapper.getValidBotSenderById(senderId);
        List<BotSender> otherSenderList = botSenderMapper.getBotSenderByCondition(new BotSenderQuery().setKookGuildId(sourceSender.getKookGuildId()).setStatus(0));
        BotSender ktvSender = otherSenderList.stream().filter(sender -> Objects.equals(sender.getName(), "KTV")).findFirst().orElse(null);
        Asserts.notNull(ktvSender, "无KTV");
        Long channelId = ktvSender.getKookChannelId();

        if(playerProcess != null && playerProcess.isAlive() && Objects.equals(this.playerChannelId, channelId)) {
            log.info("无需切换播放器");
            return;
        }

        if (playerProcess != null) {
            log.info("重启播放器");
            playerProcess.destroy();
        }
        // cmd命令拼接，注意命令中存在空格
        String command = "/home/www/lib/khl-voice -t 1/MTMzOTY=/i2q8M4KvMWCWwndT9ask7Q== -i rtmp://121.5.247.29:1935/live/livestream -c " + channelId;
        log.info("khl-voice命令：" + command);

        // 运行cmd命令，获取其进程
        this.playerProcess = Runtime.getRuntime().exec(command);
        this.playerChannelId = channelId;
    }
}
