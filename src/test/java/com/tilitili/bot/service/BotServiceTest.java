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
				"{\"player\":{\"uuid\":\"544dab96-2300-3b74-b257-96726807931c\",\"displayName\":\"§9粟什辻§r\",\"port\":5745,\"exhaustion\":1.1834042,\"exp\":0.61571187,\"whitelisted\":false,\"banned\":false,\"op\":false,\"balance\":0.0},\"quitMessage\":\"VYIX_SSS left the game\",\"eventType\":\"PlayerQuit\"}");
	}
}