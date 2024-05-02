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
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(2L), "{\"self_id\":1701008067,\"user_id\":545459363,\"time\":1714653237,\"message_id\":-2147442335,\"real_id\":-2147442335,\"message_type\":\"group\",\"sender\":{\"user_id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"card\":\"\",\"role\":\"admin\"},\"raw_message\":\"投票管理员@Cirno\",\"font\":14,\"sub_type\":\"normal\",\"message\":[{\"data\":{\"text\":\"投票管理员\"},\"type\":\"text\"},{\"data\":{\"text\":\"@Cirno\"},\"type\":\"text\"}],\"message_format\":\"array\",\"post_type\":\"message\",\"group_id\":698034152}");
	}

	@Test
	public void testHandle() throws Exception {
		BaseMessageHandle handle = messageHandleMap.get("playFishGameHandle");
		System.out.println(handle.handleMessageNew(BotMessageActionUtil.buildEmptyAction("抛竿", 181L, 4490L, 2L)));
	}
}