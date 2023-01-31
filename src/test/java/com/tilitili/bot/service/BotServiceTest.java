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
		tester.testHandleMessage(BotEnum.CIRNO_GUILD, "{\"post_type\":\"notice\",\"notice_type\":\"guild_channel_recall\",\"time\":1675139391,\"self_id\":536657454,\"guild_id\":\"49134681639135681\",\"channel_id\":\"43227251\",\"operator_id\":\"144115218679720063\",\"message_id\":\"BACuj7uNQKnBAAAAAAKTmHMAAAAAAAAQBA==\",\"self_tiny_id\":\"144115218680351893\",\"user_id\":144115218679720063}");
	}
}