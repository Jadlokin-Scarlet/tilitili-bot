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
		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":580,\"t\":\"AT_MESSAGE_CREATE\",\"id\":\"AT_MESSAGE_CREATE:4e306801-968a-45ee-b3b2-4954b5d5bdc2\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"1940475\",\"content\":\"\\u003c@!6528917200927240171\\u003e /夸夸我\",\"guild_id\":\"13343424136567982074\",\"id\":\"08fa8f889ce4b1d796b90110fbb77638db044882b69ea906\",\"member\":{\"joined_at\":\"2021-12-08T20:50:41+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"4\",\"10056905\",\"27\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=3fTZaaFczyjE81GeodXnCQ\\u0026kti=ZSebAwAAAAE\\u0026s=0\\u0026t=1688383096\",\"bot\":true,\"id\":\"6528917200927240171\",\"username\":\"琪露诺小助手\"}],\"seq\":603,\"seq_in_channel\":\"603\",\"timestamp\":\"2023-10-12T15:06:42+08:00\"}}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}