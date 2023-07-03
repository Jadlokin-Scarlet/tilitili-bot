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

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":4,\"t\":\"AT_MESSAGE_CREATE\",\"id\":\"AT_MESSAGE_CREATE:0b1299ea-1156-43f0-8eee-2793d7722a88\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"1733719\",\"content\":\"\\u003c@!6528917200927240171\\u003e /关注 2321030\",\"guild_id\":\"13343424136567982074\",\"id\":\"08fa8f889ce4b1d796b90110d7e86938f40448ccd089a506\",\"member\":{\"joined_at\":\"2021-12-08T20:50:41+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"4\",\"10056905\",\"26\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=SpibFjiadcwQjmNIQr32DA8w\\u0026kti=ZKJoJQAAAAI\\u0026s=0\\u0026t=1685528653\",\"bot\":true,\"id\":\"6528917200927240171\",\"username\":\"琪露诺小助手\"}],\"seq\":628,\"seq_in_channel\":\"628\",\"timestamp\":\"2023-07-03T14:18:52+08:00\"}}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}