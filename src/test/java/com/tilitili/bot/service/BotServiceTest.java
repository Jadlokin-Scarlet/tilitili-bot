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
		tester.testHandleMessage(BotEnum.CIRNO_GUILD, "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1674830919,\"self_id\":536657454,\"sub_type\":\"channel\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"周\",\"tiny_id\":\"144115218679720063\",\"user_id\":144115218679720063},\"guild_id\":\"49134681639135681\",\"channel_id\":\"43227251\",\"message_id\":\"BACuj7uNQKnBAAAAAAKTmHMAAAAAAAAOFQ==\",\"user_id\":\"144115218679720063\",\"message\":\"[CQ:image,file=547461e7ccc0ab5386fa78807405ee11.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-3084665644-547461E7CCC0AB5386FA78807405EE11/0?term=255]\"}");
	}
}