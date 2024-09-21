package com.tilitili.bot.util;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Component
public class BotMessageActionUtil {
	@Resource
	private BotSessionService botSessionService;

	public BotMessageAction buildEmptyAction(String message) {
        return this.buildEmptyAction(message, null);
    }
    public BotMessageAction buildEmptyAction(String message, Long userId) {
        List<BotMessageChain> chainList = Collections.singletonList(BotMessageChain.ofPlain(message));
        return new BotMessageAction(new BotMessage().setBotMessageChainList(chainList).setBotUser(new BotUserDTO().setId(userId)), null);
    }

	public BotMessageAction buildEmptyAction(String message, Long userId, Long senderId, Long botId) {
		BotRobot bot = new BotRobot().setId(botId);
		List<BotMessageChain> chainList = Collections.singletonList(BotMessageChain.ofPlain(message));
		return new BotMessageAction(new BotMessage().setBotMessageChainList(chainList)
				.setBotUser(new BotUserDTO().setId(userId))
				.setBotSender(new BotSender().setId(senderId))
				.setBot(bot), null);
	}

	public BotMessageAction buildEmptyAction(String message, BotRobot bot, BotSender botSender, BotUserDTO botUser) {
		List<BotMessageChain> chainList = Collections.singletonList(BotMessageChain.ofPlain(message));
		return new BotMessageAction(new BotMessage().setBotMessageChainList(chainList)
				.setBotUser(botUser)
				.setBotSender(botSender)
				.setBot(bot), botSessionService.getSession(botSender.getId()));
	}
}
