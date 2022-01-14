package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class VoiceHandle extends ExceptionRespMessageHandle {
    private final BaiduManager baiduManager;
    private final MiraiManager miraiManager;

    @Autowired
    public VoiceHandle(BaiduManager baiduManager, MiraiManager miraiManager) {
        this.baiduManager = baiduManager;
        this.miraiManager = miraiManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws IOException, InterruptedException {
        File wavFile = new File("/home/admin/silk/voice.wav");
        File slkFile = new File("/home/admin/silk/voice.slk");

        if (wavFile.exists()) Asserts.isTrue(wavFile.delete(), "删除wav失败");
        if (slkFile.exists()) Asserts.isTrue(slkFile.delete(), "删除slk失败");

        String text = messageAction.getBody();

        if (text == null) {
            return null;
        }

        String jpText = baiduManager.translate("jp", text);

        log.info("jpText="+jpText);

        String speakShell = String.format("sh /home/admin/silk/run.sh %s", jpText);
        Runtime.getRuntime().exec(speakShell);

        Thread.sleep(1000);

        Asserts.isTrue(slkFile.exists(), "转码slk失败");

        String voiceId = miraiManager.uploadVoice(slkFile);
        Asserts.notBlank(voiceId, "上传失败");

        return BotMessage.simpleVoiceIdMessage(voiceId);
    }
}
