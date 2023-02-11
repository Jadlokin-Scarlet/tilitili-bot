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
				"{\"syncId\":\"-1\",\"data\":{\"type\":\"FriendMessage\",\"messageChain\":[{\"type\":\"Source\",\"id\":16453,\"time\":1676121346},{\"type\":\"Plain\",\"text\":\"相似推荐\"},{\"type\":\"Image\",\"imageId\":\"{91AAF214-E1A1-3700-BA19-D51A417B2A3F}.jpg\",\"url\":\"http://c2cpicdw.qpic.cn/offpic_new/0//545459363-789069041-91AAF214E1A13700BA19D51A417B2A3F/0?term=2\",\"path\":null,\"base64\":null,\"width\":1080,\"height\":1142,\"size\":155860,\"imageType\":\"JPG\",\"isEmoji\":false}],\"sender\":{\"id\":545459363,\"nickname\":\"Jadlokin_Scarlet\",\"remark\":\"Jadlokin_Scarlet\"}}}");
	}
}