package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.emnus.BotEnum;
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

	@Test
	public void syncHandleEvent() {
	}

	@Test
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.CIRNO_KOOK, "{\"s\":0,\"d\":{\"channel_type\":\"GROUP\",\"type\":9,\"target_id\":\"7818625731373538\",\"author_id\":\"1240983673\",\"content\":\"ping\",\"extra\":{\"type\":9,\"code\":\"\",\"guild_id\":\"7240290365245748\",\"channel_name\":\"\\u6587\\u5b57\\u9891\\u9053\",\"author\":{\"id\":\"1240983673\",\"username\":\"Jadlokin_Scarlet\",\"identify_num\":\"0574\",\"online\":true,\"os\":\"Android\",\"status\":1,\"avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg\\/icon\",\"vip_avatar\":\"https:\\/\\/img.kookapp.cn\\/avatars\\/2022-12\\/jguK9547k60dw0dw.jpg\\/icon\",\"banner\":\"\",\"nickname\":\"Jadlokin_Scarlet\",\"roles\":[],\"is_vip\":false,\"is_ai_reduce_noise\":true,\"is_personal_card_bg\":false,\"bot\":false,\"decorations_id_map\":{\"join_voice\":10024,\"avatar_border\":10065}},\"visible_only\":null,\"mention\":[],\"mention_all\":false,\"mention_roles\":[],\"mention_here\":false,\"nav_channels\":[],\"kmarkdown\":{\"raw_content\":\"ping\",\"mention_part\":[],\"mention_role_part\":[],\"channel_part\":[]},\"last_msg_content\":\"Jadlokin_Scarlet\\uff1aping\",\"send_msg_device\":0},\"msg_id\":\"201bfa42-827f-4127-bc7c-6da3e8d40886\",\"msg_timestamp\":1674756912789,\"nonce\":\"xydU7ybE7pNAoENohTj57iwD\",\"from_type\":1},\"extra\":{\"verifyToken\":\"meqedpKNlCwHMka3\",\"encryptKey\":\"\",\"callbackUrl\":\"\"},\"sn\":7}");
	}
}