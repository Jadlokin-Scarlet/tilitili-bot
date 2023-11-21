package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.BotRobot;
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
		BotRobot bot = botRobotCacheManager.getBotRobotById(3L);
		tester.testHandleMessage(bot, "{\"post_type\":\"message\",\"message_type\":\"group\",\"time\":1700594114,\"self_id\":536657454,\"sub_type\":\"normal\",\"user_id\":545459363,\"message_id\":-523584208,\"anonymous\":null,\"group_id\":698034152,\"sender\":{\"age\":0,\"area\":\"\",\"card\":\"\",\"level\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"role\":\"owner\",\"sex\":\"unknown\",\"title\":\"\",\"user_id\":545459363},\"raw_message\":\"[CQ:at,qq=2854212978] 抽老婆\",\"font\":0,\"message\":\"[CQ:at,qq=2854212978] 抽老婆\",\"message_seq\":23077}");
//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}