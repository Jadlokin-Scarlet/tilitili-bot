package com.tilitili.bot.service.mirai.translate;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTranslateMapping;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotTranslateMappingMapper;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddTranslateConfigHandle extends ExceptionRespMessageHandle {
	private final BotTranslateMappingMapper botTranslateMappingMapper;

	@Autowired
	public AddTranslateConfigHandle(BotTranslateMappingMapper botTranslateMappingMapper) {
		this.botTranslateMappingMapper = botTranslateMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		String senderIdStr = messageAction.getBody("senderId");
		Long senderId = StringUtils.isBlank(senderIdStr)? botSender.getId(): Long.valueOf(senderIdStr);
		String value = messageAction.getValueOrDefault("");
		String from = messageAction.getBodyOrDefault("from", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): null);
		String to = messageAction.getBodyOrDefault("to", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		BotTranslateMapping oldTranslateMapping = botTranslateMappingMapper.getBotTranslateMappingBySenderIdAndSource(senderId, from);
		if (oldTranslateMapping != null) {
			String oldTo = oldTranslateMapping.getResult();
			oldTranslateMapping.setResult(to);
			botTranslateMappingMapper.updateBotTranslateMappingSelective(oldTranslateMapping);
			return BotMessage.simpleTextMessage(String.format("覆盖了%s的翻译！从%s变成%s。", from, oldTo, to));
		}

		BotTranslateMapping translateMapping = new BotTranslateMapping();
		translateMapping.setSenderId(senderId);
		translateMapping.setSource(from);
		translateMapping.setResult(to);
		botTranslateMappingMapper.addBotTranslateMappingSelective(translateMapping);
		return BotMessage.simpleTextMessage("学费啦！");
	}
}
