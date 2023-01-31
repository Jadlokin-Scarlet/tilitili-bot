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
		tester.testHandleMessage(BotEnum.MINECRAFT, "{\"player\":{\"uuid\":\"34e35969-8c80-37c9-9257-57c7672ccfbe\",\"displayName\":\"§3§k1§5帅帅§3§k1§r\",\"port\":4498,\"exhaustion\":0.02523899,\"exp\":0.42857128,\"whitelisted\":false,\"banned\":false,\"op\":true,\"balance\":0.0},\"playerName\":\"§3§k1§5帅帅§3§k1§r\",\"message\":\"kkt TachibanaHinan0\",\"eventType\":\"PlayerChat\"}");
	}
}