package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class BotRobotServiceTest {
	@Autowired
	private BotRobotService tester;

	@Test
	void list() {
		//tester.list();
	}

	@Test
	void upBot() {
		tester.upBot(18L);
	}

	@Test
	void downBot() {
		//tester.downBot();
	}

	@Test
	void addBot() {
		//tester.addBot();
	}

	@Test
	void deleteBot() {
		//tester.deleteBot();
	}

	@Test
	void editBot() {
		//tester.editBot();
	}

	@Test
	void getBot() {
		//tester.getBot();
	}
}