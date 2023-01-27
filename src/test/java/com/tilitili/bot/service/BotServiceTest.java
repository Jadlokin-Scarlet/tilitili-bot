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
		tester.testHandleMessage(BotEnum.CIRNO_GUILD, "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1674814670,\"self_id\":536657454,\"sub_type\":\"channel\",\"guild_id\":\"49134681639135681\",\"channel_id\":\"43227251\",\"message_id\":\"BACuj7uNQKnBAAAAAAKTmHMAAAAAAAAN+A==\",\"user_id\":\"144115218678395873\",\"message\":\"[CQ:image,file=cf73d17de6ce0e6e3b9a59824b5eab51.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2560874704-CF73D17DE6CE0E6E3B9A59824B5EAB51/0?term=255][CQ:image,file=3e70f6b2df9962b9e963abcab807fc1e.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2336677848-3E70F6B2DF9962B9E963ABCAB807FC1E/0?term=255][CQ:image,file=bba79240cd84e1bd50f89fc53a9b3d67.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2577252800-BBA79240CD84E1BD50F89FC53A9B3D67/0?term=255][CQ:image,file=e17541832217ddf08e7b601b26109ea9.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2748763124-E17541832217DDF08E7B601B26109EA9/0?term=255][CQ:image,file=8c7a14147e9d1d3c39142114faec404b.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2416248342-8C7A14147E9D1D3C39142114FAEC404B/0?term=255][CQ:image,file=33f0db6720544cfbb5164dbf4b61a449.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2467951624-33F0DB6720544CFBB5164DBF4B61A449/0?term=255][CQ:image,file=88f708278409b9a5b91004c06ea7f5e3.image,url=https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2934039062-88F708278409B9A5B91004C06EA7F5E3/0?term=255]\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"糸雪\",\"tiny_id\":\"144115218678395873\",\"user_id\":144115218678395873}}");
	}
}