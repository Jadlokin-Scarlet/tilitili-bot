package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
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
	BotRobotMapper botRobotMapper;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotMapper.getBotRobotById(9L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":152,\"t\":\"AT_MESSAGE_CREATE\",\"id\":\"AT_MESSAGE_CREATE:7a8fc6f1-6bc5-4e85-81a7-5559a037d761\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/16532940906de46a670e4153d1?t=1653294090\",\"bot\":false,\"id\":\"7809560624645512060\",\"username\":\"诺氟\"},\"channel_id\":\"1109095\",\"content\":\"\\u003c@!6528917200927240171\\u003e /关注  23210308\",\"guild_id\":\"5237615478283154023\",\"id\":\"08e7e49ea9f0f0efd74810e7d84338d471488fc1faa406\",\"member\":{\"joined_at\":\"2021-12-13T16:26:02+08:00\",\"nick\":\"诺氟\",\"roles\":[\"2\",\"11522093\",\"33\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=gHhXaHoEJZqN18FrcOwGZg\\u0026kti=ZJ6gfwAAAAI\\u0026s=0\\u0026t=1685528653\",\"bot\":true,\"id\":\"6528917200927240171\",\"username\":\"琪露诺小助手\"}],\"seq\":14548,\"seq_in_channel\":\"14548\",\"timestamp\":\"2023-06-30T17:29:51+08:00\"}}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}