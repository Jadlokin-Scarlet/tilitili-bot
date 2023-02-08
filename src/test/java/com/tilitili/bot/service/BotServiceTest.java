package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.emnus.BotEnum;
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
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.CIRNO_QQ,
				"{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":13989,\"time\":1675861800},{\"type\":\"Plain\",\"text\":\"测试\"}],\"sender\":{\"id\":545459363,\"memberName\":\"Jadlokin_Scarlet\",\"specialTitle\":\"\",\"permission\":\"OWNER\",\"joinTimestamp\":1645168075,\"lastSpeakTimestamp\":1675861800,\"muteTimeRemaining\":0,\"group\":{\"id\":698034152,\"name\":\"My Homo告警群\",\"permission\":\"MEMBER\"}}}}");
	}
}