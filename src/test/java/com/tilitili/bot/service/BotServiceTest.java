package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.utils.TimeUtil;
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
		tester.syncHandleTextMessage("{\"syncId\":\"-1\",\"data\":{\"type\":\"FriendMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":38072,\"time\":1670205887},{\"type\":\"Plain\",\"text\":\"ping\"}],\"sender\":{\"id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"remark\":\"Jadlokin_Scarlet\"}}}", BotEmum.CIRNO_QQ);
		TimeUtil.millisecondsSleep(3600);
	}
}