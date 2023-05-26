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
		tester.testHandleMessage(bot, "{\"op\":0,\"s\":3,\"t\":\"MESSAGE_CREATE\",\"id\":\"MESSAGE_CREATE:08fa8f889ce4b1d796b90110d7e869388b0448a4d9c4a306\",\"d\":{\"author\":{\"avatar\":\"https://qqchannel-profile-1251316161.file.myqcloud.com/165242847412697fcd16614b9c?t=1652428474\",\"bot\":false,\"id\":\"1792232868513015736\",\"username\":\"Jadlokin_Scarlet\"},\"channel_id\":\"1733719\",\"content\":\"\\u003c@!6528917200927240171\\u003e ping\",\"guild_id\":\"13343424136567982074\",\"id\":\"08fa8f889ce4b1d796b90110d7e869388b0448a4d9c4a306\",\"member\":{\"joined_at\":\"2021-12-08T20:50:41+08:00\",\"nick\":\"Jadlokin_Scarlet\",\"roles\":[\"4\",\"10056905\",\"25\"]},\"mentions\":[{\"avatar\":\"http://thirdqq.qlogo.cn/g?b=oidb\\u0026k=Dfst7sFg4IWYFzUqmicqqHA\\u0026kti=ZHEspAAAAAA\\u0026s=0\\u0026t=1685007874\",\"bot\":true,\"id\":\"6528917200927240171\",\"username\":\"琪露诺-测试中\"}],\"seq\":523,\"seq_in_channel\":\"523\",\"timestamp\":\"2023-05-27T06:03:16+08:00\"}}");
	}
}