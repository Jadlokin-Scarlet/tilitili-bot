package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TransactionHandle extends ExceptionRespMessageToSenderHandle {
	private final BotItemMapper botItemMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotUserMapper botUserMapper;

	@Autowired
	public TransactionHandle(BotItemMapper botItemMapper, BotUserItemMappingManager botUserItemMappingManager, BotUserMapper botUserMapper) {
		this.botItemMapper = botItemMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botUserMapper = botUserMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKey();
		switch (key) {
			case "兑换": return handleBuy(messageAction);
			case "出售": return handleSell(messageAction);
			default: throw new AssertException("啊嘞，不对劲");
		}
	}

	private BotMessage handleSell(BotMessageAction messageAction) {
		String value = messageAction.getValue();
		List<String> valueList = Arrays.asList(value.split(" "));
		Asserts.notEmpty(valueList, "格式错啦(物品名)");
		String itemName = valueList.get(0);
		Integer itemNum;
		if (valueList.size() > 1) {
			String itemNumStr = valueList.get(1);
			Asserts.isNumber(itemNumStr, "格式错啦(物品数量)");
			itemNum = Integer.parseInt(itemNumStr);
		} else {
			itemNum = null;
		}

		BotUser botUser = messageAction.getBotUser();
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
//		botUserItemMappingManager.safeSellItem(botUser.getId(), botItem.getId(), itemNum);

		String numMessage = itemNum == null ? "" : itemNum + "个";
		String nowScore = String.valueOf(botUserMapper.getBotUserById(botUser.getId()).getScore());
		return BotMessage.simpleTextMessage(String.format("兑换掉%s成功，剩余积分%s。", numMessage + itemName, nowScore));
	}

	private BotMessage handleBuy(BotMessageAction messageAction) {
		String value = messageAction.getValue();
		List<String> valueList = Arrays.asList(value.split(" "));
		Asserts.notEmpty(valueList, "格式错啦(物品名)");
		String itemName = valueList.get(0);
		String itemNumStr = valueList.size() > 1 ? valueList.get(1) : "1";
		Asserts.isNumber(itemNumStr, "格式错啦(物品数量)");
		int itemNum = Integer.parseInt(itemNumStr);

		BotUser botUser = messageAction.getBotUser();
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		botUserItemMappingManager.safeBuyItem(botUser.getId(), botItem.getId(), itemNum);

		String numMessage = itemNum == 1 ? "" : itemNumStr + "个";
		String nowScore = String.valueOf(botUserMapper.getBotUserById(botUser.getId()).getScore());
		return BotMessage.simpleTextMessage(String.format("兑换%s成功，剩余积分%s。", numMessage + itemName, nowScore));
	}
}
