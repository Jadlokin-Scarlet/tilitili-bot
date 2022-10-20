package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TransactionHandle extends ExceptionRespMessageToSenderHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String value = messageAction.getValue();
		List<String> valueList = Arrays.asList(value.split(" "));
		Asserts.notEmpty(valueList, "格式错啦(物品名)");
		String itemName = valueList.get(0);
		String itemNum = valueList.size() > 1 ? valueList.get(1) : "1";
		Asserts.isNumber(itemNum, "格式错啦(物品数量)");

		return null;
	}
}
