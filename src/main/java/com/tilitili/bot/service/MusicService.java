package com.tilitili.bot.service;

import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Slf4j
@Service
public class MusicService {
    // ffmpeg位置，最好写在配置文件中
    String ffmpegPath = "/lib/";
    private Process process;

    @Async
    public void asyncPushVideoAsRTSP(String musicUrl) {
        try {
            File file = File.createTempFile("music-cloud-", ".mp3");
            HttpClientUtil.downloadFile(musicUrl, file);

            // 视频切换时，先销毁进程，全局变量Process process，方便进程销毁重启，即切换推流视频
            if(process != null){
                process.destroy();
                System.out.println(">>>>>>>>>>推流视频切换<<<<<<<<<<");
            }
            // cmd命令拼接，注意命令中存在空格
            String command = ffmpegPath; // ffmpeg位置
            command += "ffmpeg -re"; // ffmpeg开头，-re代表按照帧率发送，在推流时必须有
            command += " -i " + file.getPath(); // 指定要推送的视频
            command += " -vn -f flv rtmp://localhost/live/livestream"; // 指定推送服务器，-f：指定格式
            System.out.println("ffmpeg推流命令：" + command);

            // 运行cmd命令，获取其进程
            process = Runtime.getRuntime().exec(command);
            // 输出ffmpeg推流日志
            BufferedReader br= new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                System.out.println("视频推流信息[" + line + "]");
            }
            System.out.println("结果"+process.waitFor());
        } catch (Exception e) {
            log.warn("推流失败", e);
        }
    }

}
