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
		tester.testHandleTextMessage("{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1674670112,\"self_id\":2325622370,\"sub_type\":\"channel\",\"message\":\"[CQ:image,file=5fa56fac3e987fac7526537952e9abcf.image,url=https://gchat.qpic.cn/qmeetpic/79033321637042471/8580811-3151789254-5FA56FAC3E987FAC7526537952E9ABCF/0?term=255]\",\"self_tiny_id\":\"144115219183125111\",\"sender\":{\"nickname\":\"傲寒sky\",\"tiny_id\":\"144115218866871126\",\"user_id\":144115218866871126},\"guild_id\":\"79033321637042471\",\"channel_id\":\"8580811\",\"message_id\":\"BAEYyGMlmFknAAAAAACC7ssAAAAAAAEQtA==\",\"user_id\":\"144115218866871126\"}", BotEnum.HONG_PING);
	}
}