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
	public void syncHandleTextMessage() {
		tester.testHandleMessage(BotEnum.CIRNO_QQ,
				"{\"syncId\":\"-1\",\"data\":{\"type\":\"FriendMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":16951,\"time\":1676128339},{\"type\":\"Quote\",\"id\":-14369,\"senderId\":1701008067,\"targetId\":545459363,\"groupId\":0,\"origin\":[{\"type\":\"Plain\",\"text\":\"页数: 1\\npid: 58231596\\n[图片]\"}]},{\"type\":\"Plain\",\"text\":\"相似推荐\"}],\"sender\":{\"id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"remark\":\"Jadlokin_Scarlet\"}}}");
	}
}