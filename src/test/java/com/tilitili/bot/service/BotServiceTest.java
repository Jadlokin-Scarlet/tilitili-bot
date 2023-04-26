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
		tester.testHandleMessage(bot, "{\"syncId\":\"-1\",\"data\":{\"type\":\"FriendMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":38812,\"time\":1682490023},{\"type\":\"Plain\",\"text\":\"关注b博 425535005\"}],\"sender\":{\"id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"remark\":\"Jadlokin_Scarlet\"}}}");
	}
}