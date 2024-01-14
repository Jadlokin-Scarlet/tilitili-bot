package com.tilitili.bot.socket;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class WebSocketFactoryTest {
	@Autowired
	private WebSocketFactory tester;

	@Test
	void upBotBlocking() {
		tester.upBotBlocking(22L);
		TimeUtil.millisecondsSleep(10000);
	}
}