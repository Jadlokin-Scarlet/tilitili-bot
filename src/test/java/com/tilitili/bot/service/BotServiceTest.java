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

		tester.testHandleMessage(bot, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"8734923371624662\",\"author_id\":\"1240983673\",\"content\":\"\\u70b9\\u6b4c\\n[https:\\/\\/music.163.com\\/dj?id=2505523434&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505523434&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505499341&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505499341&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505550482&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505550482&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505483404&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505483404&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505510352&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505510352&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505523435&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505523435&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505499342&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505499342&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505535362&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505535362&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505488411&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505488411&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505542483&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505542483&userid=361260659)\\n[https:\\/\\/music.163.com\\/dj?id=2505466324&userid=361260659](https:\\/\\/music.163.com\\/dj?id=2505466324&userid=361260659)\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"1019881181624881\",\"channel_name\":\"\\u778e\\u5bc4\\u628a\\u8bf4\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[4306147],\"is_vip\":false,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":{\"join_voice\":10024},\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"\\u70b9\\u6b4c\\nhttps:\\/\\/music.163.com\\/dj?id=2505523434&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505499341&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505550482&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505483404&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505510352&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505523435&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505499342&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505535362&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505488411&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505542483&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505466324&userid=361260659\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"emoji\":[],\"last_msg_content\":\"Jadlokin_Scarlet\\uff1a\\u70b9\\u6b4c\\nhttps:\\/\\/music.163.com\\/dj?id=2505523434&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505499341&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505550482&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505483404&userid=361260659\\nhttps:\\/\\/music.163.com\\/dj?id=2505510352&userid=361260659\\nhttps:\\/\\/music.163\",\"send_msg_device\":0},\"msg_id\":\"e3be1d5c-1b07-4ce0-825b-b0282fc86e41\",\"msg_timestamp\":1685523895790,\"nonce\":\"HCJGeGIuSDD4hbBR763SBoeA\",\"from_type\":1},\"extra\":{\"verifyToken\":\"UFM9otCZrYq6Nk_r\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":6}");

//		tester.testHandleMessage(botRobotMapper.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}