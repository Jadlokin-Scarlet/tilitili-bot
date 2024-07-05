package com.tilitili.bot.service.mirai;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotItemService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.entity.BotIcePrice;
import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotIcePriceManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.mapper.mysql.BotUserItemMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TransactionHandle extends ExceptionRespMessageToSenderHandle {
	private final BotItemMapper botItemMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotIcePriceManager botIcePriceManager;
	private final BotUserManager botUserManager;
	private final BotItemService botItemService;
	private final BotUserItemMappingManager botUserItemMappingManager;

	@Autowired
	public TransactionHandle(BotItemMapper botItemMapper, BotUserItemMappingManager botUserItemMappingManager, BotUserItemMappingMapper botUserItemMappingMapper, BotIcePriceManager botIcePriceManager, BotUserManager botUserManager, BotItemService botItemService) {
		this.botItemMapper = botItemMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botIcePriceManager = botIcePriceManager;
		this.botUserManager = botUserManager;
		this.botItemService = botItemService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "兑换": return handleBuy(messageAction);
			case "回收": return handleSell(messageAction);
			case "背包": return handleBag(messageAction);
			case "道具": return handleItemInfo(messageAction);
			case "使用": return handleUse(messageAction);
			default: throw new AssertException("啊嘞，不对劲");
		}
	}

	private BotMessage handleUse(BotMessageAction messageAction) {
		String itemName = messageAction.getValue();

		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥。");

		boolean success = botItemService.useItem(messageAction.getBotSender(), messageAction.getBotUser(), botItem);
		Asserts.isTrue(success, "使用失败惹");
		return BotMessage.simpleTextMessage("好惹。").setPrivateSend(true);
	}

	private BotMessage handleItemInfo(BotMessageAction messageAction) {
		String itemName = messageAction.getValue();
		BotItem botItem = this.getBotItemByNameOrIce(itemName);
		Asserts.notNull(botItem, "那是啥");
		List<String> resultList = new ArrayList<>();
		resultList.add("*" + botItem.getName() + "*");
		resultList.add(botItem.getDescription());
		if (botItem.getGrade() != null) {
			resultList.add("稀有度：" + botItem.getGrade());
		}
		if (Objects.equals(botItem.getPrice(), botItem.getSellPrice())) {
			if (botItem.getPrice() == null) {
				resultList.add("无法交易");
			} else {
				resultList.add("价值：" + botItem.getPrice());
			}
		} else {
			if (botItem.getPrice() == null) {
				resultList.add("无法兑换");
			} else {
				resultList.add("兑换价：" + botItem.getPrice());
			}
			if (botItem.getSellPrice() == null) {
				resultList.add("无法回收");
			} else {
				resultList.add("回收价：" + botItem.getSellPrice());
			}
		}
		if (botItem.getMaxLimit() != null) {
			resultList.add("最大持有：" + botItem.getMaxLimit());
		}
		if (botItem.getEndTime() != null) {
			resultList.add("有效期至：" + DateUtils.formatDateYMDHMS(botItem.getEndTime()));
		}
		if (botItem.getIcon() == null) {
			return BotMessage.simpleTextMessage(String.join("\n", resultList)).setPrivateSend(true);
		} else {
			return BotMessage.simpleImageTextMessage(String.join("\n", resultList) + "\n", botItem.getIcon()).setPrivateSend(true);
		}
	}

	private BotMessage handleBag(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		List<BotItemDTO> itemList = botUserItemMappingMapper.getItemListByUserId(botUser.getId());
//		List<String> resultList = new ArrayList<>();
		if (itemList.isEmpty()) {
			return BotMessage.simpleTextMessage("背包里一尘不染。。");
		}
//		for (BotItemDTO botItemDTO : itemList) {
//			if ("背包".equals(botItemDTO.getBag())) {
//				resultList.add(botItemDTO.getName() + (botItemDTO.getNum() > 1? "*" + botItemDTO.getNum(): ""));
//			}
//		}
		Map<String, String> bagMap = itemList.stream().collect(Collectors.groupingBy(BotItemDTO::getBag, Collectors.collectingAndThen(Collectors.toList(),
				botItemDTOList -> botItemDTOList.stream().map(botItemDTO -> botItemDTO.getName() + (botItemDTO.getNum() > 1 ? "*" + botItemDTO.getNum() : "")).collect(Collectors.joining("、"))
		)));
//		String bag = String.join("、", resultList);

		List<String> bag = Lists.newArrayList(bagMap.get("背包"));
		bagMap.forEach((bagName, otherBag) -> {
			if (!"背包".equals(bagName)){
				bag.add(String.format("%s：%s", bagName, otherBag));
			}
		});
		return BotMessage.simpleTextMessage(String.join("\n", bag)).setPrivateSend(true);
	}

	private BotMessage handleSell(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notNull(botUser.getScore(), "未绑定");
		String value = messageAction.getValue();
		Asserts.notBlank(value, "格式错啦(物品名)");
		List<BotItemDTO> itemList = new ArrayList<>();

		String[] itemStrList = value.split("[ *，,、]");
		for (String itemStr : itemStrList) {
			if (StringUtils.isNumber(itemStr)) {
				itemList.get(itemList.size() - 1).setNum(Integer.parseInt(itemStr));
			} else {
				itemList.add(new BotItemDTO().setName(itemStr).setNum(1));
			}
		}


//		if (value.contains(" ")) {
//			return BotMessage.simpleTextMessage("格式错啦，详情请看(帮助 回收)");
//		} else {
//			String[] itemStrList = value.split("[，,、]");
//			itemList = Arrays.stream(itemStrList).map(itemStr -> new BotItemDTO()
//					.setName(itemStr.split("\\*")[0])
//					.setNum(itemStr.contains("*") ? Integer.parseInt(itemStr.split("\\*")[1]) : 1)
//			).collect(Collectors.toList());
//		}

		if (itemList.size() == 1) {
			BotItemDTO item = itemList.get(0);
			String itemName = item.getName();
			BotItem botItem = botItemMapper.getBotItemByName(itemName);
			Asserts.notNull(botItem, "那是啥");
			Integer itemNum = item.getNum();
			int subNum = this.sellItemWithIce(botUser.getId(), botItem.getId(), itemNum, itemName);

			String numMessage = (-subNum) + "个";
			String nowScore = String.valueOf(botUserManager.getValidBotUserByIdWithParent(botUser.getId()).getScore());
			return BotMessage.simpleTextMessage(String.format("回收%s成功，剩余可用积分%s。", numMessage + item.getName(), nowScore)).setPrivateSend(true);
		} else {
			int subNum = 0;
			for (BotItemDTO item : itemList) {
				try {
					String itemName = item.getName();
					BotItem botItem = botItemMapper.getBotItemByName(itemName);
					Asserts.notNull(botItem, "那是啥");
					Integer itemNum = item.getNum();
					if (itemNum != 0) {
						subNum += this.sellItemWithIce(botUser.getId(), botItem.getId(), itemNum, itemName);
					}
				} catch (AssertException e) {
					log.warn("物品"+item.getName()+"回收失败，"+e.getMessage());
				}
			}

			if (subNum == 0) {
				return BotMessage.simpleTextMessage("似乎没有东西可以回收");
			}

			String nowScore = String.valueOf(botUserManager.getValidBotUserByIdWithParent(botUser.getId()).getScore());
			return BotMessage.simpleTextMessage(String.format("回收成功，剩余可用积分%s。", nowScore)).setPrivateSend(true);
		}
	}

	private BotMessage handleBuy(BotMessageAction messageAction) {
		String value = messageAction.getValue();
		Asserts.notBlank(value, "格式错啦(物品名)");
		List<String> valueList = Arrays.asList(value.split("[ *]"));
		Asserts.notEmpty(valueList, "格式错啦(物品名)");
		String itemName = valueList.get(0);
		String itemNumStr = valueList.size() > 1 ? valueList.get(1) : "1";
		Asserts.isNumber(itemNumStr, "格式错啦(物品数量)");
		int itemNum = Integer.parseInt(itemNumStr);

		BotUserDTO botUser = messageAction.getBotUser();
		Asserts.notNull(botUser.getScore(), "未绑定");
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		Asserts.notNull(botItem, "那是啥");
		Integer subNum = this.buyItemWithIce(botUser.getId(), botItem.getId(), itemNum, itemName);

		String numMessage = subNum == 1 ? "" : subNum + "个";
		String nowScore = String.valueOf(botUserManager.getValidBotUserByIdWithParent(botUser.getId()).getScore());
		return BotMessage.simpleTextMessage(String.format("兑换%s成功，剩余积分%s。", numMessage + itemName, nowScore)).setPrivateSend(true);
	}

	private Integer buyItemWithIce(Long userId, Long itemId, Integer itemNum, String itemName) {
		SafeTransactionDTO data = new SafeTransactionDTO().setUserId(userId).setItemId(itemId).setItemNum(itemNum);
		if (BotItemConstant.ICE_NAME.equalsIgnoreCase(itemName)) {
			Asserts.isTrue(botUserItemMappingManager.checkBuyTime(), "周日才能兑换哦。");
			Integer price = botIcePriceManager.getIcePrice().getBasePrice();
			data.setPrice(price);
		}
		return botUserItemMappingManager.safeBuyItem(data).getSubNum();
	}

	private Integer sellItemWithIce(Long userId, Long itemId, Integer itemNum, String itemName) {
		SafeTransactionDTO data = new SafeTransactionDTO().setUserId(userId).setItemId(itemId).setItemNum(itemNum);
		if (BotItemConstant.ICE_NAME.equalsIgnoreCase(itemName)) {
			Asserts.isTrue(botUserItemMappingManager.checkSellTime(), "周日不收哦。");
			Integer price = botIcePriceManager.getIcePrice().getPrice();
			data.setSellPrice(price);
		}
		return botUserItemMappingManager.safeSellItem(data).getSubNum();
	}

	private BotItem getBotItemByNameOrIce(String itemName) {
		BotItem botItem = botItemMapper.getBotItemByName(itemName);
		if (BotItemConstant.ICE_NAME.equalsIgnoreCase(itemName)) {
			BotIcePrice icePrice = botIcePriceManager.getIcePrice();
			if (botUserItemMappingManager.checkBuyTime()) {
				botItem.setPrice(icePrice.getBasePrice());
			}
			if (botUserItemMappingManager.checkSellTime()) {
				botItem.setSellPrice(icePrice.getPrice());
			}
		}
		return botItem;
	}
}
