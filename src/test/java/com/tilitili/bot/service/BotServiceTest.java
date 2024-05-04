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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(24L), "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"5458467331657836\",\"author_id\":\"1240983673\",\"content\":\"ping\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"7240290365245748\",\"guild_type\":0,\"channel_name\":\"\\u6d4b\\u8bd5\\u9891\\u9053\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[],\"is_vip\":false,\"vip_amp\":false,\"bot\":false,\"nameplate\":[],\"decorations_id_map\":{\"join_voice\":10024,\"background\":10208},\"is_sys\":false},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"ping\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[],\"spl\":[]},\"emoji\":[],\"last_msg_content\":\"Jadlokin_Scarlet\\uff1aping\",\"send_msg_device\":2},\"msg_id\":\"5622a39e-7da2-4caa-b095-239bb4ba20aa\",\"msg_timestamp\":1714842131380,\"nonce\":\"1714842130854\",\"from_type\":1},\"extra\":{\"verifyToken\":\"TDL6pBx66bwXbyfo\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":1}");
	}

	@Test
	public void testHandle() throws Exception {
		BaseMessageHandle handle = messageHandleMap.get("playFishGameHandle");
		System.out.println(handle.handleMessageNew(BotMessageActionUtil.buildEmptyAction("抛竿", 181L, 4490L, 2L)));
	}
}