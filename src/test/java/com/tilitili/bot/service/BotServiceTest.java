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
		tester.testHandleMessage(bot, "{\"op\":0,\"s\":2,\"t\":\"MESSAGE_CREATE\",\"id\":\"MESSAGE_CREATE:08fa8f889ce4b1d796b90110d7e8693886044892d0c4a306\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"1733719\",\"content\":\"ping\",\"guild_id\":\"13343424136567982074\",\"id\":\"08fa8f889ce4b1d796b90110d7e8693886044892d0c4a306\",\"member\":{\"joined_at\":\"2021-12-08T20:50:41+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"4\",\"10056905\",\"25\"]},\"seq\":518,\"seq_in_channel\":\"518\",\"timestamp\":\"2023-05-27T05:43:46+08:00\"}}");
	}
}