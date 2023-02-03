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
		tester.testHandleMessage(BotEnum.MINECRAFT,
				"{\"player\":{\"uuid\":\"85bee4ae-0d36-3c26-ba60-651531963f8a\",\"displayName\":\"§b琪露诺§r\",\"port\":1977,\"exhaustion\":0.0,\"exp\":0.0,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"playerName\":\"§b琪露诺§r\",\"message\":\"摸摸头@cirno\",\"eventType\":\"PlayerChat\"}");
	}
}