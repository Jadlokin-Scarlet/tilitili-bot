package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.BotUserItemMapping;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class TransactionHandle extends ExceptionRespMessageToSenderHandle {
	private final BotItemMapper botItemMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotUserMapper botUserMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;

	@Autowired
	public TransactionHandle(BotItemMapper botItemMapper, BotUserItemMappingManager botUserItemMappingManager, BotUserMapper botUserMapper, BotUserItemMappingMapper botUserItemMappingMapper) {
		this.botItemMapper = botItemMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botUserMapper = botUserMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKey();
		switch (key) {
			case "兑换": return handleBuy(messageAction);
			case "回收": return handleSell(messageAction);
			case "背包": return handleBag(messageAction);
			case "道具": return handleItemInfo(messageAction);
			default: throw new AssertException("啊嘞，不对劲");
		}
	}

	private BotMessage handleItemInfo(BotMessageAction messageAction) {
		String itemName = messageAction.getValue();
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥");
		List<BotMessageChain> resultList = new ArrayList<>();
		resultList.add(BotMessageChain.ofPlain("*" + botItem.getName() + "*\n"));
		resultList.add(BotMessageChain.ofPlain(botItem.getDescription() + "\n"));
		if (Objects.equals(botItem.getPrice(), botItem.getSellPrice())) {
			resultList.add(BotMessageChain.ofPlain("价值：" + botItem.getPrice() + "\n"));
		} else {
			resultList.add(BotMessageChain.ofPlain("兑换价：" + botItem.getPrice() + "\n"));
			if (botItem.getSellPrice() == 0) {
				resultList.add(BotMessageChain.ofPlain("无法回收\n"));
			} else {
				resultList.add(BotMessageChain.ofPlain("回收价：" + botItem.getSellPrice() + "\n"));
			}
		}
		resultList.add(BotMessageChain.ofPlain("最大持有：" + botItem.getMaxLimit()));
		return BotMessage.simpleListMessage(resultList);
	}

	private BotMessage handleBag(BotMessageAction messageAction) {
		BotUser botUser = messageAction.getBotUser();
		List<BotItemDTO> itemList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
		List<String> resultList = new ArrayList<>();
		if (itemList.isEmpty()) {
			return BotMessage.simpleTextMessage("背包里一尘不染。。");
		}
		for (BotItemDTO botItemDTO : itemList) {
			resultList.add(botItemDTO.getName() + (botItemDTO.getNum() > 1? "*" + botItemDTO.getNum(): ""));
		}
		return BotMessage.simpleTextMessage(String.join("，", resultList));
	}

	private BotMessage handleSell(BotMessageAction messageAction) {
		String value = messageAction.getValue();
		List<String> valueList = Arrays.asList(value.split(" "));
		Asserts.notEmpty(valueList, "格式错啦(物品名)");
		String itemName = valueList.get(0);

		BotUser botUser = messageAction.getBotUser();
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥");
		Integer itemNum;
		if (valueList.size() > 1) {
			String itemNumStr = valueList.get(1);
			Asserts.isNumber(itemNumStr, "格式错啦(物品数量)");
			itemNum = Integer.parseInt(itemNumStr);
		} else {
			BotUserItemMapping oldMapping = botUserItemMappingMapper.getBotUserItemMappingByUserIdAndItemId(botUser.getId(), botItem.getId());
			Asserts.notNull(oldMapping,  "你还没有%s。", botItem.getName());
			itemNum = oldMapping.getNum();
		}
		botUserItemMappingManager.safeSellItem(botUser.getId(), botItem.getId(), itemNum);

		String numMessage = itemNum + "个";
		String nowScore = String.valueOf(botUserMapper.getBotUserById(botUser.getId()).getScore());
		return BotMessage.simpleTextMessage(String.format("回收%s成功，剩余积分%s。", numMessage + itemName, nowScore));
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
		Asserts.notNull(botItem, "那是啥");
		botUserItemMappingManager.safeBuyItem(botUser.getId(), botItem.getId(), itemNum);

		String numMessage = itemNum == 1 ? "" : itemNumStr + "个";
		String nowScore = String.valueOf(botUserMapper.getBotUserById(botUser.getId()).getScore());
		return BotMessage.simpleTextMessage(String.format("兑换%s成功，剩余积分%s。", numMessage + itemName, nowScore));
	}
}
