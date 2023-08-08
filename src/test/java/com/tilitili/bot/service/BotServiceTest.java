package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.BotRobotCacheManager;
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
	@Resource
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(3L);

		tester.testHandleMessage(bot, "{\"post_type\":\"message\",\"message_type\":\"group\",\"time\":1691477297,\"self_id\":536657454,\"sub_type\":\"normal\",\"group_id\":698034152,\"message\":\"[CQ:at,qq=536657454] 深色橡木树场在哪儿\",\"message_seq\":21533,\"sender\":{\"age\":0,\"area\":\"\",\"card\":\"\",\"level\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"role\":\"owner\",\"sex\":\"unknown\",\"title\":\"\",\"user_id\":545459363},\"user_id\":545459363,\"message_id\":789165645,\"anonymous\":null,\"raw_message\":\"[CQ:at,qq=536657454] 深色橡木树场在哪儿\",\"font\":0}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}