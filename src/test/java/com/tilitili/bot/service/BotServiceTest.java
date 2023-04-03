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
		BotRobot bot = botRobotMapper.getBotRobotById(1L);
		tester.testHandleMessage(bot, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"7818625731373538\",\"author_id\":\"1240983673\",\"content\":\"\\u6478\\u8138\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"7240290365245748\",\"channel_name\":\"\\u5f00\\u9ed1\\u5566bot\\u6d4b\\u8bd5\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[],\"is_vip\":false,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":{\"join_voice\":10024}},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"\\u6478\\u8138\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"last_msg_content\":\"Jadlokin_Scarlet\\uff1a\\u6478\\u8138\",\"send_msg_device\":0},\"msg_id\":\"57a3a413-72f8-47c9-9e05-cc3d4e0c512c\",\"msg_timestamp\":1676149245782,\"nonce\":\"IUpGMvtx1w5fIIu3y4BL8fB4\",\"from_type\":1},\"extra\":{\"verifyToken\":\"meqedpKNlCwHMka3\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":3}");
	}
}