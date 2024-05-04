package com.tilitili.bot.service.mirai;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class PlayTwentyOneHandleTest {
	@Autowired
	private PlayTwentyOneHandle tester;
	@Autowired
	BotRobotCacheManager botRobotCacheManager;
	@Autowired
	BotSenderCacheManager botSenderCacheManager;
	@Autowired
	BotUserManager botUserManager;

	private BotMessage botMessage;

	@BeforeEach
	void init() {
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(2L);
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(181L);
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(3417L);
		botMessage = new BotMessage()
				.setBotUser(botUser)
				.setBotSender(botSender)
				.setBot(bot);
	}

	@Test
	void handleMessage() {
		BotMessage zbResp = testHandle("准备 10");

		String botLine = zbResp.getBotMessageChainList().get(0).getText();
		String playerLine = zbResp.getBotMessageChainList().get(1).getText();

		int botNumber = Integer.parseInt(StringUtils.patten1(",(\\d{1,2})", botLine));
		int playerNumber1 = Integer.parseInt(StringUtils.patten1("：(\\d{1,2})", playerLine));
		int playerNumber2 = Integer.parseInt(StringUtils.patten1(",(\\d{1,2})", playerLine));
	}

	private BotMessage testHandle(String message) {
		botMessage.setBotMessageChainList(Collections.singletonList(BotMessageChain.ofPlain(message)));
		return tester.handleMessage(new BotMessageAction(botMessage, null));
	}


}