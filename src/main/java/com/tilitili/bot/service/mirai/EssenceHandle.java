package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.CheckManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EssenceHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final CheckManager checkManager;

	@Autowired
	public EssenceHandle(BotManager botManager, CheckManager checkManager) {
		this.botManager = botManager;
		this.checkManager = checkManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotEnum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		String quoteMessageId = messageAction.getQuoteMessageId();
		Asserts.notNull(quoteMessageId, "格式错啦(回复)");
		BotMessage quoteMessage = messageAction.getQuoteMessage();
		Asserts.notNull(quoteMessage, "找不到该消息");
		String text = quoteMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_PLAIN))
				.map(BotMessageChain::getText).collect(Collectors.joining(""));
		if (checkManager.checkText(text)) {
			botManager.setEssence(bot, botSender, quoteMessageId);
			return BotMessage.emptyMessage();
		} else {
			return BotMessage.simpleTextMessage("达咩");
		}
	}
}
