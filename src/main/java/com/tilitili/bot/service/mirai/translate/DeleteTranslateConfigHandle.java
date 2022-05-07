package com.tilitili.bot.service.mirai.translate;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTranslateMapping;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotTranslateMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteTranslateConfigHandle extends ExceptionRespMessageHandle {
	private final BotTranslateMappingMapper botTranslateMappingMapper;

	@Autowired
	public DeleteTranslateConfigHandle(BotTranslateMappingMapper botTranslateMappingMapper) {
		this.botTranslateMappingMapper = botTranslateMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		Long senderId = botSender.getId();
		String value = messageAction.getValueOrDefault("");
		String from = messageAction.getBodyOrDefault("from", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): null);

		BotTranslateMapping oldTranslateMapping = botTranslateMappingMapper.getBotTranslateMappingBySenderIdAndSource(senderId, from);
		Asserts.notNull(oldTranslateMapping, "这个我还不会啦！");

		botTranslateMappingMapper.deleteBotTranslateMappingByPrimary(oldTranslateMapping.getId());
		return BotMessage.simpleTextMessage("移除了。");
	}
}
