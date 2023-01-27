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
	public void syncHandleEvent() {
	}

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.MINECRAFT, "{\"player\":{\"uuid\":\"dbba8ea0-4acd-3919-88ec-f9adf72a8f2d\",\"displayName\":\"Fantasy233\",\"address\":\"58.48.238.187\",\"port\":14366,\"exhaustion\":0.905427,\"exp\":0.62690437,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"playerName\":\"Fantasy233\",\"message\":\"签到\",\"eventType\":\"PlayerChat\"}");
	}
}