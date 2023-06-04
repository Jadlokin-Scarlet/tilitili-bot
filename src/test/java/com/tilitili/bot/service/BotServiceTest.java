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
		BotRobot bot = botRobotMapper.getBotRobotById(8L);

		tester.testHandleMessage(bot, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"8734923371624662\",\"author_id\":\"376335742\",\"content\":\"\\u70b9\\u6b4c [https:\\/\\/music.163.com\\/playlist?id=4955285530&userid=376472001](https:\\/\\/music.163.com\\/playlist?id=4955285530&userid=376472001)\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"1019881181624881\",\"channel_name\":\"\\u70b9\\u6b4c\\u673a\",\"author\":{\"id\":\"376335742\",\"username\":\"\\u968f\\u610f\",\"identify_num\":\"0317\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2021-12\\/jnL5T8AMbW02s02s.jpg?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2021-12\\/jnL5T8AMbW02s02s.jpg?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"\\u968f\\u610f\",\"roles\":[],\"is_vip\":true,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":{\"background\":10167},\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"\\u70b9\\u6b4c https:\\/\\/music.163.com\\/playlist?id=4955285530&userid=376472001\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"emoji\":[],\"last_msg_content\":\"\\u968f\\u610f\\uff1a\\u70b9\\u6b4c https:\\/\\/music.163.com\\/playlist?id=4955285530&userid=376472001\",\"send_msg_device\":1},\"msg_id\":\"72fd8cbb-ce30-4d9d-b1e1-1f4c2b3160d4\",\"msg_timestamp\":1685887653888,\"nonce\":\"nF2tzD8m6yPID6Xtzw3Cf7R8\",\"from_type\":1},\"extra\":{\"verifyToken\":\"UFM9otCZrYq6Nk_r\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":26}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}