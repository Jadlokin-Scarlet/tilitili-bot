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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(3L), "{\"post_type\":\"message\",\"message_type\":\"group\",\"time\":1707034996,\"self_id\":536657454,\"sub_type\":\"normal\",\"message\":\"桀桀\",\"raw_message\":\"桀桀\",\"user_id\":3011769107,\"anonymous\":null,\"group_id\":907741387,\"sender\":{\"age\":0,\"area\":\"\",\"card\":\"我们臆念禾一\",\"level\":\"\",\"nickname\":\"Karyl\",\"role\":\"member\",\"sex\":\"unknown\",\"title\":\"\",\"user_id\":3011769107},\"message_id\":-1704016291,\"font\":0,\"message_seq\":909592}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}
}