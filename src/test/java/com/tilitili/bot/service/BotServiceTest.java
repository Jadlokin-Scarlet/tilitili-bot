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
@EnableAutoConfiguration(exclude = DubboAutoConfiguration.class)
public class BotServiceTest {
	@Resource
	BotService tester;
	@Resource
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(19L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":59,\"t\":\"MESSAGE_CREATE\",\"id\":\"MESSAGE_CREATE:08f0c7c691f686c0e7f10110cbdd8b0438aa890f48ceb9b4a806\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"8580811\",\"content\":\"2\",\"guild_id\":\"17424145721190360048\",\"id\":\"08f0c7c691f686c0e7f10110cbdd8b0438aa890f48ceb9b4a806\",\"member\":{\"joined_at\":\"2021-12-15T23:23:55+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"2\",\"10050759\",\"35\"]},\"seq\":246954,\"seq_in_channel\":\"246954\",\"timestamp\":\"2023-09-22T12:49:18+08:00\"}}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}