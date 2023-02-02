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
//		tester.testHandleMessage(BotEnum.CIRNO_QQ,
//				"{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":143838,\"time\":1675318243},{\"type\":\"Plain\",\"text\":\"赠送 冰淇淋\"}],\"sender\":{\"id\":782036280,\"memberName\":\"Somnus\",\"specialTitle\":\"\",\"permission\":\"MEMBER\",\"joinTimestamp\":1583808958,\"lastSpeakTimestamp\":1675318243,\"muteTimeRemaining\":0,\"group\":{\"id\":902813629,\"name\":\"\uD83E\uDD3A\",\"permission\":\"MEMBER\"}}}}");
	}
}