package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.manager.TemplateImageManager;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextImageHandle extends BaseMessageHandleAdapt {
	private final TemplateImageManager templateImageManager;

	@Autowired
	public TextImageHandle(TemplateImageManager templateImageManager) {
		this.templateImageManager = templateImageManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String text = messageAction.getBody();
		String imageUrl = templateImageManager.getLongStringImage(text);
		return BotMessage.simpleImageMessage(imageUrl);
	}
}
