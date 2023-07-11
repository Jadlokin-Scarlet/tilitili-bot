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
		BotRobot bot = botRobotMapper.getBotRobotById(19L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":23,\"t\":\"AT_MESSAGE_CREATE\",\"id\":\"AT_MESSAGE_CREATE:44d58d8c-4fca-483a-a21f-be42b0f65d21\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165335717938a04b0f07fcb553?t=1653357179\",\"bot\":false,\"id\":\"5834723721567985948\",\"username\":\"\uD800\uDC82\"},\"channel_id\":\"6470157\",\"content\":\"\\u003c@!9511287328637479520\\u003e /关注 1\",\"guild_id\":\"5237615478283154023\",\"id\":\"08e7e49ea9f0f0efd748108df48a0338fbe60148abc0ada506\",\"member\":{\"joined_at\":\"2023-04-17T17:00:27+08:00\",\"nick\":\"\uD800\uDC82\",\"roles\":[\"2\",\"28\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=PZ1IuiasnYFU7ic5LW0gzRibQ\\u0026kti=ZKtgKwAAAAA\\u0026s=0\\u0026t=1688654877\",\"bot\":true,\"id\":\"9511287328637479520\",\"username\":\"虹频bot-测试中\"}],\"seq\":29563,\"seq_in_channel\":\"29563\",\"timestamp\":\"2023-07-10T09:34:35+08:00\"}}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}