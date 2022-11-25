package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EssenceHandle extends BaseMessageHandleAdapt {
	private final BotManager botManager;

	@Autowired
	public EssenceHandle(BotManager botManager) {
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotEmum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		String quoteMessageId = messageAction.getQuoteMessageId();
		Asserts.notNull(quoteMessageId, "格式错啦(回复)");
		botManager.setEssence(bot, botSender, quoteMessageId);
		return BotMessage.emptyMessage();
	}
}
