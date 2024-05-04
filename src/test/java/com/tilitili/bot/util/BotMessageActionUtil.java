package com.tilitili.bot.util;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;

import java.util.Collections;
import java.util.List;

public class BotMessageActionUtil {
    public static BotMessageAction buildEmptyAction(String message) {
        return BotMessageActionUtil.buildEmptyAction(message, null);
    }
    public static BotMessageAction buildEmptyAction(String message, Long userId) {
        List<BotMessageChain> chainList = Collections.singletonList(BotMessageChain.ofPlain(message));
        return new BotMessageAction(new BotMessage().setBotMessageChainList(chainList).setBotUser(new BotUserDTO().setId(userId)), null);
    }

	public static BotMessageAction buildEmptyAction(String message, Long userId, Long senderId, Long botId) {
		BotRobot bot = new BotRobot().setId(botId);
		List<BotMessageChain> chainList = Collections.singletonList(BotMessageChain.ofPlain(message));
		return new BotMessageAction(new BotMessage().setBotMessageChainList(chainList)
				.setBotUser(new BotUserDTO().setId(userId))
				.setBotSender(new BotSender().setId(senderId))
				.setBot(bot), null);
	}
}
