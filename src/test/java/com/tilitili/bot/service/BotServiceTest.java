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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(3L), "{\"post_type\":\"message\",\"message_type\":\"group\",\"time\":1702975738,\"self_id\":536657454,\"sub_type\":\"normal\",\"anonymous\":null,\"message\":\"[CQ:at,qq=2010851224] [CQ:at,qq=2854212978] 比划比划\",\"message_seq\":38989,\"raw_message\":\"[CQ:at,qq=2010851224] [CQ:at,qq=2854212978] 比划比划\",\"font\":0,\"group_id\":413438226,\"sender\":{\"age\":0,\"area\":\"\",\"card\":\"\",\"level\":\"\",\"nickname\":\"\uD83C\uDF65雪\",\"role\":\"admin\",\"sex\":\"unknown\",\"title\":\"\",\"user_id\":571790751},\"user_id\":571790751,\"message_id\":-948845572}");
		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
		tester.testHandleMessage(bot, "{\"op\":0,\"s\":3,\"t\":\"GROUP_AT_MESSAGE_CREATE\",\"id\":\"GROUP_AT_MESSAGE_CREATE:ebb1uclrakugrelcmdvdcg4g3llpt4qise4znd0coixcb3ifop7ft9skolij8yha\",\"d\":{\"author\":{\"id\":\"F2236D4C1EDA60136A2727B753CA2407\",\"member_openid\":\"F2236D4C1EDA60136A2727B753CA2407\"},\"content\":\"  比划比划\",\"group_id\":\"C6A1F9AB51F8F50B754C200C084B99EC\",\"group_openid\":\"C6A1F9AB51F8F50B754C200C084B99EC\",\"id\":\"ROBOT1.0_ebb1UCLraKUGrelcmDVdCKbrQ2WQMRmyrtrXVSoZYxOpRERAWEkYqFEaiXF6itGYrDHuaD37J5zVfzxxHrXn0w!!\",\"timestamp\":\"2023-12-19T16:48:58+08:00\"}}");
	}
}