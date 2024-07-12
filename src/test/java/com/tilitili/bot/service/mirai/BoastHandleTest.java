package com.tilitili.bot.service.mirai;

import com.tilitili.bot.BotApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
public class BoastHandleTest {
	@Resource
	BoastHandle tester;

	@Test
	public void handleMessage() {
//		String result = HttpClientUtil.httpGet("https://api.shadiao.app/chp");
//		String read = (String) JSONPath.read(result, "$.data.text", String.class);
//		System.out.println();
	}
}