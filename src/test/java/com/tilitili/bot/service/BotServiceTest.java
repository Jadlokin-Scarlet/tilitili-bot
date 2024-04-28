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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "{\"self_id\":1701008067,\"user_id\":1067473881,\"time\":1714298736,\"message_id\":-2147454796,\"real_id\":-2147454796,\"message_type\":\"group\",\"sender\":{\"user_id\":1067473881,\"nickname\":\"追忆さん\",\"card\":\"吃井人\",\"role\":\"member\"},\"raw_message\":\"[CQ:reply,id=-2147454797]zt\",\"font\":14,\"sub_type\":\"normal\",\"message\":[{\"data\":{\"id\":\"-2147454797\"},\"type\":\"reply\"},{\"data\":{\"text\":\"zt\"},\"type\":\"text\"}],\"message_format\":\"array\",\"post_type\":\"message\",\"group_id\":757886689}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}
}