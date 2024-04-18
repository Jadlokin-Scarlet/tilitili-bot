package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
@EnableAutoConfiguration(exclude={DubboAutoConfiguration.class})
public class BotServiceTest {
	@Resource
	BotService tester;
	@Resource
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":941441,\"time\":1712763623},{\"type\":\"Plain\",\"text\":\"3*(7+5/5)\"}],\"sender\":{\"id\":972683489,\"memberName\":\"希卡利大助手\",\"specialTitle\":\"超级金发控\",\"permission\":\"MEMBER\",\"joinTimestamp\":1648650588,\"lastSpeakTimestamp\":1712763623,\"muteTimeRemaining\":0,\"group\":{\"id\":907741387,\"name\":\"公主连结AKB48#素质游戏\",\"permission\":\"ADMINISTRATOR\"},\"active\":null}}}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}
}