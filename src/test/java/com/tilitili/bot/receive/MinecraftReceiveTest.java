package com.tilitili.bot.receive;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
public class MinecraftReceiveTest {
	@Resource
	MinecraftReceive tester;

	@Test
	public void receiveMinecraftMessage() {
	}
}