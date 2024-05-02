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
		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(24L), "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"7818625731373538\",\"author_id\":\"1240983673\",\"content\":\"(met)1744765512(met) puser\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"7240290365245748\",\"guild_type\":0,\"channel_name\":\"\\u5f00\\u9ed1\\u5566bot\\u6d4b\\u8bd5\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Websocket\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2024-02\\/IdlyxmKR5K06j06j.png?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[],\"is_vip\":false,\"vip_amp\":false,\"bot\":false,\"nameplate\":[],\"decorations_id_map\":{\"join_voice\":10024,\"background\":10208},\"is_sys\":false},\"visible_only\":null,\"mention\":[\"1744765512\"],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"@\\u742a\\u9732\\u8bfakana puser\",\"mention_part\":[{\"id\":\"1744765512\",\"username\":\"\\u742a\\u9732\\u8bfakana\",\"full_name\":\"\\u742a\\u9732\\u8bfakana#1009\",\"avatar\":\"https:\\/\\/img.kookapp.cn\\/assets\\/2024-01\\/Z9OEU9JczO0rs0rs.jpg?x-oss-process=style\\/icon\"}],\"mention_role_part\":[],\"channel_part\":[],\"spl\":[]},\"emoji\":[],\"quote\":{\"id\":\"66312408e9a824bb320dedd8\",\"rong_id\":\"a37e17b8-9c00-443b-ae10-8a9e4baefe87\",\"type\":10,\"content\":\"[{\\\"theme\\\":\\\"secondary\\\",\\\"color\\\":\\\"\\\",\\\"size\\\":\\\"lg\\\",\\\"expand\\\":false,\\\"modules\\\":[{\\\"type\\\":\\\"section\\\",\\\"mode\\\":\\\"left\\\",\\\"accessory\\\":null,\\\"text\\\":{\\\"type\\\":\\\"plain-text\\\",\\\"emoji\\\":true,\\\"content\\\":\\\"\\u9875\\u6570: 1\\\\npid: 53416620\\\\n\\\",\\\"elements\\\":[]},\\\"elements\\\":[]},{\\\"type\\\":\\\"container\\\",\\\"elements\\\":[{\\\"type\\\":\\\"image\\\",\\\"src\\\":\\\"https:\\\\\\/\\\\\\/img.kookapp.cn\\\\\\/attachments\\\\\\/2024-05\\\\\\/01\\\\\\/wMn5jD4GZZ1jk0m8.jpeg\\\",\\\"alt\\\":\\\"\\\",\\\"size\\\":\\\"lg\\\",\\\"circle\\\":false,\\\"elements\\\":[]}]}],\\\"type\\\":\\\"card\\\"}]\",\"interact_res\":null,\"create_at\":1714496520546,\"author\":{\"id\":\"1744765512\",\"username\":\"\\u742a\\u9732\\u8bfakana\",\"identify_num\":\"1009\",\"online\":true,\"os\":\"Websocket\",\"status\":0,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/assets\\/2024-01\\/Z9OEU9JczO0rs0rs.jpg?x-oss-process=style\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/assets\\/2024-01\\/Z9OEU9JczO0rs0rs.jpg?x-oss-process=style\\/icon\",\"banner\":\"\",\"nickname\":\"\\u742a\\u9732\\u8bfakana\",\"roles\":[31360429],\"is_vip\":false,\"vip_amp\":false,\"bot\":true,\"nameplate\":[],\"bot_status\":0,\"tag_info\":{\"color\":\"#0096FF\",\"bg_color\":\"#0096FF33\",\"text\":\"\\u673a\\u5668\\u4eba\"},\"mobile_verified\":true,\"is_sys\":false,\"client_id\":\"eYVcTm18G9q75BPx\",\"verified\":false,\"joined_at\":1707038202000,\"active_time\":1714639674658},\"can_jump\":true},\"last_msg_content\":\"Jadlokin_Scarlet\\uff1a@\\u742a\\u9732\\u8bfakana puser\",\"send_msg_device\":1},\"msg_id\":\"a4003735-3d6b-48c6-bbb8-17bd3bff4459\",\"msg_timestamp\":1714639875924,\"nonce\":\"d9htxSMgLeBhBw3jl8MQ7Pjm\",\"from_type\":1},\"extra\":{\"verifyToken\":\"TDL6pBx66bwXbyfo\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":157}");
//		BotRobot bot = botRobotCacheManager.getBotRobotById(9L);
//		tester.testHandleMessage(bot, "");
	}

	@Test
	public void testHandle() throws Exception {
		BaseMessageHandle handle = messageHandleMap.get("playFishGameHandle");
		System.out.println(handle.handleMessageNew(BotMessageActionUtil.buildEmptyAction("抛竿", 181L, 4490L, 2L)));
	}
}