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
		tester.testHandleMessage(bot, "{\"syncId\":\"-1\",\"data\":{\"type\":\"GroupMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":52662,\"time\":1682012918},{\"type\":\"At\",\"target\":1701008067,\"display\":\"\"},{\"type\":\"Plain\",\"text\":\" 还活着吗\"}],\"sender\":{\"id\":1578611368,\"memberName\":\"随意\",\"specialTitle\":\"\",\"permission\":\"MEMBER\",\"joinTimestamp\":1664609583,\"lastSpeakTimestamp\":1682012918,\"muteTimeRemaining\":0,\"group\":{\"id\":756782332,\"name\":\"我的评价是，润！\",\"permission\":\"OWNER\"}}}}");
	}
}