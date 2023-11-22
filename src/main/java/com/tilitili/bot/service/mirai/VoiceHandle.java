package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.CheckManager;
import com.tilitili.common.manager.VitsOPManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class VoiceHandle extends ExceptionRespMessageHandle {
    private final BotManager botManager;
    private final VitsOPManager vitsOPManager;
    private final CheckManager checkManager;
    private final MusicService musicService;

    @Autowired
    public VoiceHandle(BotManager botManager, VitsOPManager vitsOPManager, CheckManager checkManager, MusicService musicService) {
        this.botManager = botManager;
        this.vitsOPManager = vitsOPManager;
        this.checkManager = checkManager;
        this.musicService = musicService;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws IOException, InterruptedException {
        BotRobot bot = messageAction.getBot();
        BotSender botSender = messageAction.getBotSender();
        BotUserDTO botUser = messageAction.getBotUser();
        String speaker = messageAction.getParamOrDefault("who", "派蒙");
        String speed = messageAction.getParamOrDefault("speed", "1.2");

        File wavFile = new File("/home/admin/silk/voice.wav");
//        File slkFile = new File("/home/admin/silk/voice.slk");

        if (wavFile.exists()) Asserts.isTrue(wavFile.delete(), "删除wav失败");
//        if (slkFile.exists()) Asserts.isTrue(slkFile.delete(), "删除slk失败");

        String text = messageAction.getValueOrDefault(messageAction.getBody());
        Asserts.isTrue(checkManager.checkText(text), "达咩");


        if (text == null) {
            return null;
        }

        Asserts.isTrue(VitsOPManager.nameList.contains(speaker), "%s是谁啊。", speaker);
        String url = vitsOPManager.getVoiceUrl(text, speaker, speed);
//        url = String.format("https://dds.dui.ai/runtime/v1/synthesize?voiceId=%s&speed=%s&volume=100&audioType=wav&text=%s", speaker, speed, URLEncoder.encode(text, "UTF-8"));
        HttpClientUtil.downloadFile(url, wavFile);

//        String speakShell = String.format("sh /home/admin/silk/run2.sh %s", text);
//        Runtime.getRuntime().exec(speakShell);

//        Thread.sleep(1000);

//        Asserts.isTrue(slkFile.exists(), "转码slk失败");

        String voiceId = botManager.uploadVoice(bot, wavFile);
        Asserts.notBlank(voiceId, "上传失败");

        PlayerMusicDTO playerMusicDTO = new PlayerMusicDTO();
        playerMusicDTO.setFileUrl(url).setType(PlayerMusicDTO.TYPE_File);
        if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusicDTO) == null) {
            return BotMessage.simpleVoiceIdMessage(voiceId, url);
        } else {
            return BotMessage.emptyMessage();
        }
    }
//	@Override
//    public BotMessage handleMessage(BotMessageAction messageAction) throws IOException, InterruptedException {
//        File wavFile = new File("/home/admin/silk/voice.wav");
//        File slkFile = new File("/home/admin/silk/voice.slk");
//
//        if (wavFile.exists()) Asserts.isTrue(wavFile.delete(), "删除wav失败");
//        if (slkFile.exists()) Asserts.isTrue(slkFile.delete(), "删除slk失败");
//
//        String text = messageAction.getBody();
//
//        if (text == null) {
//            return null;
//        }
//
//        String jpText = BaiduUtil.translate("jp", text);
//
//        log.info("jpText="+jpText);
//
//        String speakShell = String.format("sh /home/admin/silk/run.sh %s", jpText);
//        Runtime.getRuntime().exec(speakShell);
//
//        Thread.sleep(1000);
//
//        Asserts.isTrue(slkFile.exists(), "转码slk失败");
//
//        String voiceId = botManager.uploadVoice(slkFile);
//        Asserts.notBlank(voiceId, "上传失败");
//
//        return BotMessage.simpleVoiceIdMessage(voiceId);
//    }
}
