package com.tilitili.bot.socket;

import com.tilitili.bot.BotApplication;
import com.tilitili.common.manager.BotManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
class GetQQGroupBotInfoWebSocketHandleTest {
    @Autowired
    private BotManager botManager;
    @Test
    public void test() throws URISyntaxException, InterruptedException {
//        BotRobot botRobot = new BotRobot()
//                .setType(BotRobotConstant.TYPE_QQ_GROUP)
//                .setPushType(BotRobotConstant.PUSH_TYPE_WS)
//                .setHost("https://api.sgroup.qq.com/")
//                .setIntents(1073741827)
//                .setVerifyKey("102050123.uKIXspEBueEWsiyIAFNoN6LaLF10Mxyf.xvoY8YoutiQyTyTw");
//        String webSocketUrl = botManager.getWebSocketUrl(botRobot);
//        String token = botManager.getToken(botRobot);
//        new GetQQGroupBotInfoWebSocketHandle(
//                new URI(webSocketUrl),
//                botRobot,
//                token
//        ).connectBlocking();
//        TimeUtil.millisecondsSleep(100000);
    }

}
/*
GetQQGroupBotInfoWebSocketHandle handle = new GetQQGroupBotInfoWebSocketHandle(
		new URI(webSocketUrl),
		botRobot,
		token
);
handle.connectBlocking();
TimeUtil.millisecondsSleep(5000);
handle.closeBlocking();

* */