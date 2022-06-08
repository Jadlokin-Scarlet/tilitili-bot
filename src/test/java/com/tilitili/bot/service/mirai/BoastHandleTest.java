package com.tilitili.bot.service.mirai;

import static org.junit.Assert.*;

import com.alibaba.fastjson.JSONPath;
import com.tilitili.bot.StartApplication;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class BoastHandleTest {
	@Resource
	BoastHandle tester;

	@Test
	public void handleMessage() {
		String result = HttpClientUtil.httpGet("https://api.shadiao.app/chp");
		String read = (String) JSONPath.read(result, "$.data.text", String.class);
		System.out.println();
	}
}