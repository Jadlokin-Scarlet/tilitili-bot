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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(20L), "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":255,\"target_id\":\"9825756437375475\",\"author_id\":\"1\",\"content\":\"[\\u7cfb\\u7edf\\u6d88\\u606f]\",\"extra\":{\"type\":\"deleted_message\",\"body\":{\"channel_id\":\"9267070914352280\",\"mention\":[],\"mention_here\":false,\"mention_roles\":[],\"mention_all\":false,\"content\":\"[{\\\"theme\\\":\\\"secondary\\\",\\\"color\\\":\\\"\\\",\\\"size\\\":\\\"lg\\\",\\\"expand\\\":false,\\\"modules\\\":[{\\\"type\\\":\\\"section\\\",\\\"mode\\\":\\\"left\\\",\\\"accessory\\\":{\\\"type\\\":\\\"image\\\",\\\"src\\\":\\\"https:\\\\\\/\\\\\\/img.kookapp.cn\\\\\\/assets\\\\\\/2024-03\\\\\\/AIhDPt8uPr03m03m.jpg\\\",\\\"alt\\\":\\\"\\\",\\\"size\\\":\\\"lg\\\",\\\"circle\\\":false,\\\"title\\\":\\\"\\\",\\\"elements\\\":[]},\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u5f53\\u524d\\u64ad\\u653e\\uff1a\\u8ff7\\u661f\\u53eb\\\",\\\"elements\\\":[]},\\\"elements\\\":[]},{\\\"type\\\":\\\"context\\\",\\\"elements\\\":[{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u4e0b\\u4e00\\u9996\\uff1a\\u96ea\\uff0c\\u4f46\\u540d\\u5b57\\u81f3\\u5c11\\u4e24\\u4e2a\\u5b57\\u7684\\u4e2a\\u4eba\\u6b4c\\u5355\\\",\\\"elements\\\":[]}]},{\\\"type\\\":\\\"action-group\\\",\\\"elements\\\":[{\\\"type\\\":\\\"button\\\",\\\"theme\\\":\\\"primary\\\",\\\"value\\\":\\\"\\u70b9\\u6b4c\\\",\\\"click\\\":\\\"return-val\\\",\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u70b9\\u6b4c\\\",\\\"elements\\\":[]},\\\"external\\\":true,\\\"elements\\\":[]},{\\\"type\\\":\\\"button\\\",\\\"theme\\\":\\\"danger\\\",\\\"value\\\":\\\"\\u7ee7\\u7eed\\\",\\\"click\\\":\\\"return-val\\\",\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u7ee7\\u7eed\\\",\\\"elements\\\":[]},\\\"external\\\":true,\\\"elements\\\":[]},{\\\"type\\\":\\\"button\\\",\\\"theme\\\":\\\"primary\\\",\\\"value\\\":\\\"\\u5207\\u6b4c\\\",\\\"click\\\":\\\"return-val\\\",\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u5207\\u6b4c\\\",\\\"elements\\\":[]},\\\"external\\\":true,\\\"elements\\\":[]},{\\\"type\\\":\\\"button\\\",\\\"theme\\\":\\\"danger\\\",\\\"value\\\":\\\"\\u6e05\\u7a7a\\\",\\\"click\\\":\\\"return-val\\\",\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u6e05\\u7a7a\\u5217\\u8868\\\",\\\"elements\\\":[]},\\\"external\\\":true,\\\"elements\\\":[]}]},{\\\"type\\\":\\\"action-group\\\",\\\"elements\\\":[{\\\"type\\\":\\\"button\\\",\\\"theme\\\":\\\"primary\\\",\\\"value\\\":\\\"\\u6b4c\\u5355\\\",\\\"click\\\":\\\"return-val\\\",\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u6211\\u7684\\u6b4c\\u5355\\\",\\\"elements\\\":[]},\\\"external\\\":true,\\\"elements\\\":[]}]}],\\\"type\\\":\\\"card\\\"}]\",\"pin\":null,\"pined_time\":null,\"type\":10,\"msg_id\":\"ca7ebf51-02ea-4719-bb93-a0979df4a3b3\",\"created_at\":1718206509686}},\"msg_id\":\"94b2430b-5d8f-4558-b372-69e1bdd2f12a\",\"msg_timestamp\":1718255518551,\"nonce\":\"\",\"from_type\":1},\"extra\":{\"verifyToken\":\"q6U83VEG4tM0Wnu8\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":4456}");
	}

	@Test
	public void testHandle() throws Exception {
		BaseMessageHandle handle = messageHandleMap.get("playFishGameHandle");
		System.out.println(handle.handleMessageNew(BotMessageActionUtil.buildEmptyAction("抛竿", 181L, 4490L, 2L)));
	}
}