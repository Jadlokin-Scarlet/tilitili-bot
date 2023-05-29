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
		BotRobot bot = botRobotMapper.getBotRobotById(2L);

		tester.testHandleMessage(bot, "{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":709885,\"time\":1685386860},{\"type\":\"Plain\",\"text\":\"我是纯移动端\"}],\"sender\":{\"id\":2125890989,\"memberName\":\"Dlight\",\"specialTitle\":\"\",\"permission\":\"MEMBER\",\"joinTimestamp\":1617942259,\"lastSpeakTimestamp\":1685386861,\"muteTimeRemaining\":0,\"group\":{\"id\":907741387,\"name\":\"公主连结AKB48@记得玩碧蓝档案\",\"permission\":\"ADMINISTRATOR\"}}}}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}