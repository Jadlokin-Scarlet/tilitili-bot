package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.manager.BotRobotCacheManager;
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
	@Resource
	BotRobotCacheManager botRobotCacheManager;

	@Test
	public void syncHandleTextMessage() {
		BotRobot bot = botRobotCacheManager.getBotRobotById(3L);

		tester.testHandleMessage(bot, "{\"post_type\":\"message\",\"message_type\":\"group\",\"time\":1691423734,\"self_id\":536657454,\"sub_type\":\"normal\",\"anonymous\":null,\"group_id\":757886689,\"message\":\"[CQ:image,file=314db052c1847c0b51794ce3eff22482.image,subType=11,url=https://gchat.qpic.cn/gchatpic_new/1095186908/757886689-2875568838-314DB052C1847C0B51794CE3EFF22482/0?term=2\\u0026amp;is_origin=0]\",\"raw_message\":\"[CQ:image,file=314db052c1847c0b51794ce3eff22482.image,subType=11,url=https://gchat.qpic.cn/gchatpic_new/1095186908/757886689-2875568838-314DB052C1847C0B51794CE3EFF22482/0?term=2\\u0026amp;is_origin=0]\",\"message_id\":-1958602413,\"font\":0,\"message_seq\":346084,\"sender\":{\"age\":0,\"area\":\"\",\"card\":\"西野花的训练师\",\"level\":\"\",\"nickname\":\"master\",\"role\":\"member\",\"sex\":\"unknown\",\"title\":\"虾头男二号\",\"user_id\":1095186908},\"user_id\":1095186908}");

//		tester.testHandleMessage(botRobotCacheManager.getBotRobotById(12L), "{\"post_type\":\"message\",\"message_type\":\"guild\",\"time\":1685139325,\"self_id\":536657454,\"sub_type\":\"channel\",\"message\":\"[CQ:at,qq=144115218753207094] ping\",\"self_tiny_id\":\"144115218680351893\",\"sender\":{\"nickname\":\"Jadlokin_Scarlet\",\"tiny_id\":\"144115218678093982\",\"user_id\":144115218678093982},\"guild_id\":\"14133921638967841\",\"channel_id\":\"1733719\",\"message_id\":\"BAAyNroSkuohAAAAAAAadFcAAAAAAAACDQ==\",\"user_id\":\"144115218678093982\"}");
	}
}