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
//		tester.testHandleMessage(BotEnum.MINECRAFT,
//				"{\"player\":{\"uuid\":\"79e3f853-d7bb-306c-8090-fa93a18445e3\",\"displayName\":\"§B猫猫§r\",\"port\":2721,\"exhaustion\":3.6167374,\"exp\":0.20207244,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"playerName\":\"§B猫猫§r\",\"message\":\"认领老婆 小米\",\"eventType\":\"PlayerChat\"}");
	}
}