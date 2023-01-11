package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.emnus.BotEmum;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class BotServiceTest {
	@Resource
	BotService tester;

	@Test
	public void syncHandleEvent() {
	}

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleTextMessage("{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":8516,\"time\":1673431411},{\"type\":\"Plain\",\"text\":\"管理员\"},{\"type\":\"At\",\"target\":545459363,\"display\":\"\"},{\"type\":\"Plain\",\"text\":\" \"}],\"sender\":{\"id\":545459363,\"memberName\":\"Jadlokin_Scarlet\",\"specialTitle\":\"测试\",\"permission\":\"MEMBER\",\"joinTimestamp\":1548094603,\"lastSpeakTimestamp\":1673431411,\"muteTimeRemaining\":0,\"group\":{\"id\":729412455,\"name\":\"My Homo\",\"permission\":\"OWNER\"}}}}", BotEmum.CIRNO_QQ);
	}
}