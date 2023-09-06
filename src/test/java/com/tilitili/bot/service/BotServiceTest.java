package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.BotRobotCacheManager;
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
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(18L);

		tester.testHandleMessage(bot, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"3775058757738607\",\"author_id\":\"1798815298\",\"content\":\"1\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"1836608360018339\",\"guild_type\":0,\"channel_name\":\"Bot\\u704c\\u6c34\",\"author\":{\"id\":\"1798815298\",\"username\":\"\\u5e05\\u5e05\",\"identify_num\":\"4832\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2021-01\\/g6VL6ih0HW02s02s.jpg?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2021-01\\/g6VL6ih0HW02s02s.jpg?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"\\u5e05\\u5e05\",\"roles\":[1073912],\"is_vip\":false,\"vip_amp\":false,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":null,\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"1\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"emoji\":[],\"last_msg_content\":\"\\u5e05\\u5e05\\uff1a1\",\"send_msg_device\":1},\"msg_id\":\"910ab5e8-46d4-43db-abe4-7dea59c75ac3\",\"msg_timestamp\":1693743121936,\"nonce\":\"tXQmKAd1hcyssDxNf1a7hgsj\",\"from_type\":1},\"extra\":{\"verifyToken\":\"rnI_DdskWkXlyHZZ\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":90}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}