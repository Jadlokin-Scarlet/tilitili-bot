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
		BotRobot bot = botRobotMapper.getBotRobotById(1L);

		tester.testHandleMessage(bot, "{\"post_type\":\"message\",\"message_type\":\"private\",\"time\":1686666874,\"self_id\":2325622370,\"sub_type\":\"friend\",\"font\":0,\"sender\":{\"age\":0,\"nickname\":\"Kiryca\",\"sex\":\"unknown\",\"user_id\":1275191194},\"message_id\":-2012143917,\"user_id\":1275191194,\"target_id\":2325622370,\"message\":\"翻译[CQ:image,file=d79ed3c5bdf242c0733baca8084a79e1.image,url=https://c2cpicdw.qpic.cn/offpic_new/1275191194//1275191194-1080520736-D79ED3C5BDF242C0733BACA8084A79E1/0?term=2\\u0026amp;is_origin=0]\",\"raw_message\":\"翻译[CQ:image,file=d79ed3c5bdf242c0733baca8084a79e1.image,url=https://c2cpicdw.qpic.cn/offpic_new/1275191194//1275191194-1080520736-D79ED3C5BDF242C0733BACA8084A79E1/0?term=2\\u0026amp;is_origin=0]\"}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}