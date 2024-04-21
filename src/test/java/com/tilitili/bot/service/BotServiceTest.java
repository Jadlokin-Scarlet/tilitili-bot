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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "{\"self_id\":1701008067,\"user_id\":545459363,\"time\":1713722201,\"message_id\":-2147478739,\"real_id\":-2147478739,\"message_type\":\"private\",\"sender\":{\"user_id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"card\":\"\"},\"raw_message\":\"帮助\",\"font\":14,\"sub_type\":\"friend\",\"message\":[{\"data\":{\"text\":\"帮助\"},\"type\":\"text\"}],\"message_format\":\"array\",\"post_type\":\"message\"}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}
}