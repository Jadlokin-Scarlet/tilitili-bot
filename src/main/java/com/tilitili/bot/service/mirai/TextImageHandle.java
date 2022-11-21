package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.TemplateImageService;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextImageHandle extends BaseMessageHandleAdapt {
	private final TemplateImageService templateImageService;

	@Autowired
	public TextImageHandle(TemplateImageService templateImageService) {
		this.templateImageService = templateImageService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String text = messageAction.getBody();
		String imageUrl = templateImageService.getLongStringImage(text);
		return BotMessage.simpleImageMessage(imageUrl);
	}
}
