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
@EnableAutoConfiguration(exclude={DubboAutoConfiguration.class})
public class BotServiceTest {
	@Resource
	BotService tester;
	@Resource
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);

		tester.testHandleMessage(bot, "{\"op\":0,\"s\":2,\"t\":\"GROUP_AT_MESSAGE_CREATE\",\"id\":\"GROUP_AT_MESSAGE_CREATE:kzluho7r7yjmdyw6aewqypveqaja5qzi75qevuv957wseidnfe0ixfc6s\",\"d\":{\"author\":{\"id\":\"DB7034DD8B2B442CF9A86B0A3AED91BB\",\"member_openid\":\"DB7034DD8B2B442CF9A86B0A3AED91BB\"},\"content\":\" ping\",\"group_id\":\"6F467EF9B93A3BB166C0E8BF9259A6FB\",\"group_openid\":\"6F467EF9B93A3BB166C0E8BF9259A6FB\",\"id\":\"ROBOT1.0_KZlUHo7R7yjmdyw6Ae-WQ9IQ4f8V5h7oyYIh7iqifx8wltqpS5ByceBTx6rHPUyVy0bzWi8-PDzwfCMemkroZw!!\",\"timestamp\":\"2023-11-22T01:40:01+08:00\"}}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}