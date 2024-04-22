package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(20L), "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"9267070914352280\",\"author_id\":\"1240983673\",\"content\":\"帮助\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"9825756437375475\",\"guild_type\":0,\"channel_name\":\"点歌台\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[7290901],\"is_vip\":false,\"vip_amp\":false,\"bot\":false,\"nameplate\":[],\"decorations_id_map\":{\"join_voice\":10024,\"background\":10208},\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"帮助\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[],\"spl\":[]},\"emoji\":[],\"last_msg_content\":\"Jadlokin_Scarlet：帮助\",\"send_msg_device\":1},\"msg_id\":\"006992ad-c6b0-476f-add4-b308a851f756\",\"msg_timestamp\":1713776862218,\"nonce\":\"PBAyXM78126TBaSiMMZ3W9wu\",\"from_type\":1},\"sn\":114}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}
}