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
		tester.testHandleTextMessage("{\"player\":{\"uuid\":\"d99bc33d-3aeb-308b-b991-6733a3ed281f\",\"displayName\":\"禾一§r\",\"address\":\"117.155.40.218\",\"port\":10813,\"exhaustion\":2.7338712,\"exp\":0.8327261,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"playerName\":\"禾一§r\",\"message\":\"永远怀念\",\"eventType\":\"PlayerChat\"}", BotEnum.MINECRAFT);
	}
}