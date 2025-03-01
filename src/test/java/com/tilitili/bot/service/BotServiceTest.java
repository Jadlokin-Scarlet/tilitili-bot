package com.tilitili.bot.service;

import com.tilitili.bot.BotApplication;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.util.BotMessageActionUtil;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
@EnableAutoConfiguration(exclude={DubboAutoConfiguration.class})
public class BotServiceTest {
	@Resource
	BotService tester;
	@Resource
	BotRobotCacheManager botRobotCacheManager;
	@Resource
	 Map<String, BaseMessageHandle> messageHandleMap;
	@Resource
	private BotSenderCacheManager botSenderCacheManager;
	@Resource
	private BotUserManager botUserManager;
	@Resource
	private BotMessageActionUtil botMessageActionUtil;

	@Test
	public void syncHandleTextMessage() {
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "");
	}

	@Test
	public void testHandle() throws Exception {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(2L);
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(3759L);
		BaseMessageHandle handle = messageHandleMap.get("QQGroupQueryHandle");
		System.out.println(handle.handleMessageNew(botMessageActionUtil.buildEmptyAction("查询 玩家4CD",
				bot, botSender, botUserManager.getValidBotUserById(3384L))));
//		System.out.println(handle.handleMessageNew(botMessageActionUtil.buildEmptyAction("测试#$", bot, botSender, botUserManager.getValidBotUserById(13L))));
//		System.out.println(handle.handleMessageNew(botMessageActionUtil.buildEmptyAction("摸鱼", bot, botSender, botUserManager.getValidBotUserById(181L))));
//		System.out.println(handle.handleMessageNew(botMessageActionUtil.buildEmptyAction("鱼呢", bot, botSender, botUserManager.getValidBotUserById(13L))));
//		System.out.println(handle.handleMessageNew(botMessageActionUtil.buildEmptyAction("收杆", bot, botSender, botUserManager.getValidBotUserById(13L))));
	}
}