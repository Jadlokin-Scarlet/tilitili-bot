package com.tilitili.bot.service.mirai;

import com.tilitili.bot.BotApplication;
import com.tilitili.bot.util.BotMessageActionUtil;
import com.tilitili.common.entity.view.bot.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
class PetpetHandleTest {
	@Autowired
	private PetpetHandle tester;

	@Test
	void handleMessage() throws Exception {
		BotMessage botMessage = tester.handleMessage(BotMessageActionUtil.buildEmptyAction("生成 osu 啊啊啊"));
		System.out.println(botMessage);
	}
}