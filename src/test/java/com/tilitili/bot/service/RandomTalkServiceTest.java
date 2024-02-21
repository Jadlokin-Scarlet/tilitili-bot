package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class RandomTalkServiceTest {
	@Autowired
	private RandomTalkService tester;

	@Test
	void listRandomTalk() {
		System.out.println(tester.listRandomTalk(new BotFunctionTalkQuery().setAdminUserId(181L).setPageSize(20).setCurrent(2)));
	}

	@Test
	void importRandomTalk() {
		//tester.importRandomTalk();
	}
}