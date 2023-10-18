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
		BotRobot bot = botRobotCacheManager.getBotRobotById(20L);

		tester.testHandleMessage(bot, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"9267070914352280\",\"author_id\":\"1240983673\",\"content\":\"\\u70b9\\u6b4c1 \\u79bb\\u6b4c\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"9825756437375475\",\"guild_type\":0,\"channel_name\":\"\\u70b9\\u6b4c\\u53f0\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[7290901],\"is_vip\":false,\"vip_amp\":false,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":{\"join_voice\":10024,\"background\":10208},\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"\\u70b9\\u6b4c1 \\u79bb\\u6b4c\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"emoji\":[],\"last_msg_content\":\"Jadlokin_Scarlet\\uff1a\\u70b9\\u6b4c1 \\u79bb\\u6b4c\",\"send_msg_device\":1},\"msg_id\":\"dc9564ef-128f-403b-92ed-cae3045b0f8e\",\"msg_timestamp\":1697628290183,\"nonce\":\"pvEzBxy5pTJsVjnPgfc2rMSa\",\"from_type\":1},\"extra\":{\"verifyToken\":\"q6U83VEG4tM0Wnu8\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":169}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}