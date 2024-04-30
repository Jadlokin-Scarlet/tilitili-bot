package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.service.mirai.base.BaseMessageHandle;
import com.tilitili.bot.util.BotMessageActionUtil;
import com.tilitili.common.manager.BotRobotCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
@EnableAutoConfiguration(exclude={DubboAutoConfiguration.class})
public class BotServiceTest {
	@Resource
	BotService tester;
	@Resource
	BotRobotCacheManager botRobotCacheManager;
	@Resource
	 Map<String, BaseMessageHandle> messageHandleMap;

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "{\"self_id\":1701008067,\"user_id\":545459363,\"time\":1714412288,\"message_id\":-2147450162,\"real_id\":-2147450162,\"message_type\":\"group\",\"sender\":{\"user_id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"card\":\"\",\"role\":\"member\"},\"raw_message\":\"投降\",\"font\":14,\"sub_type\":\"normal\",\"message\":[{\"data\":{\"text\":\"投降\"},\"type\":\"text\"}],\"message_format\":\"array\",\"post_type\":\"message\",\"group_id\":698034152}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}

	@Test
	public void testHandle() throws Exception {
		BaseMessageHandle handle = messageHandleMap.get("playFishGameHandle");
		System.out.println(handle.handleMessageNew(BotMessageActionUtil.buildEmptyAction("抛竿", 181L, 3384L, 2L)));
	}
}