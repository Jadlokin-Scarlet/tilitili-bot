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
		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":160,\"t\":\"AT_MESSAGE_CREATE\",\"id\":\"AT_MESSAGE_CREATE:9b442842-5ce9-4f6f-b9da-1f35a0cb645d\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"3675101\",\"content\":\"\\u003c@!6528917200927240171\\u003e /关注 1243082405\",\"guild_id\":\"8418653666170975154\",\"id\":\"08b2e7a4d9fc9cc4ea7410dda7e00138b9ac0448bdd9e3a506\",\"member\":{\"joined_at\":\"2022-07-13T20:04:11+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"29\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=P34G0Nn6141AuYKNVZCoYA\\u0026kti=ZLjrtgAAAAE\\u0026s=0\\u0026t=1688383096\",\"bot\":true,\"id\":\"6528917200927240171\",\"username\":\"琪露诺小助手\"}],\"seq\":71225,\"seq_in_channel\":\"71225\",\"timestamp\":\"2023-07-20T16:13:49+08:00\"}}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}