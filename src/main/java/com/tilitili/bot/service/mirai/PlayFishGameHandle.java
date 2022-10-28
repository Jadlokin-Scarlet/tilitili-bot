package com.tilitili.bot.service.mirai;

import com.tilitili.bot.component.fish.FishGame;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.FishPlayerConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.query.FishConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.mapper.mysql.*;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
//	private final FishGame fishGame;
	private final Random random;

	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final FishConfigMapper fishConfigMapper;
	private final BotItemMapper botItemMapper;
	private final BotUserMapper botUserMapper;

	@Autowired
	public PlayFishGameHandle(FishGame fishGame, FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager, FishConfigMapper fishConfigMapper, BotItemMapper botItemMapper, BotUserMapper botUserMapper) {
//		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.fishConfigMapper = fishConfigMapper;
		this.botItemMapper = botItemMapper;
		this.botUserMapper = botUserMapper;

		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKey()) {
			case "抛竿": return handleStart(messageAction);
			case "收杆": return handleEnd(messageAction);
			default: throw new AssertException();
		}
//		fishGame.addOperate(messageAction);
	}

	private BotMessage handleEnd(BotMessageAction messageAction) {
		BotUser botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		if (FishPlayerConstant.STATUS_FISHING.equals(fishPlayer.getStatus())) {
			return BotMessage.simpleTextMessage("啥也没有。。");
		}
		Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), FishPlayerConstant.STATUS_COLLECT, FishPlayerConstant.STATUS_FINALL);
		Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
		Integer scale = fishPlayer.getScale();
		List<FishConfig> configList = fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setScale(scale));
		int theRate = random.nextInt(10000);
		FishConfig fishConfig = null;
		for (FishConfig config : configList) {
			theRate -= config.getRate();
			if (theRate <= 0) {
				fishConfig = config;
				break;
			}
		}
		Asserts.notNull(fishConfig, "啊嘞，不对劲");
		fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setItemId(fishConfig.getId()));
		String description = fishConfig.getDescription();
		Long itemId = fishConfig.getItemId();
		Integer price = fishConfig.getPrice();
		Integer cost = fishConfig.getCost();

		List<BotMessageChain> resultList = new ArrayList<>();
		resultList.add(BotMessageChain.ofPlain(description));
		if (itemId != null) {
			BotItem botItem = botItemMapper.getBotItemById(itemId);
			botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(itemId).setNum(1));
			resultList.add(BotMessageChain.ofPlain(String.format("(%s+1)", botItem.getName())));
		}

		if (price != null && price > 0) {
			botUserMapper.safeUpdateScore(userId, price);
			resultList.add(BotMessageChain.ofPlain(String.format("(积分+%s)", price)));
		}

		if (cost != null) {
			BotItem botItem = botItemMapper.getBotItemById(BotItemDTO.FISH_FOOD);
			if (cost != 1) {
				botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(1 - cost));
			}
			if (cost > 0) {
				resultList.add(BotMessageChain.ofPlain(String.format("(%s-%s)", botItem.getName(), cost)));
			}
		}

		return BotMessage.simpleListMessage(resultList);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUser botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.checkNull(fishPlayer, "你已经在钓啦！");
		fishPlayer = new FishPlayer();
		fishPlayer.setSenderId(senderId);
		fishPlayer.setUserId(userId);
		fishPlayer.setStatus(FishPlayerConstant.STATUS_WAIT);
		fishPlayerMapper.addFishPlayerSelective(fishPlayer);
		List<BotMessageChain> resultList = new ArrayList<>();

		List<BotItemDTO> botItemList = botUserItemMappingMapper.getItemListByUserId(userId);
		List<Long> botItemIdList = botItemList.stream().map(BotItemDTO::getItemId).collect(Collectors.toList());

		// 兑换道具
		if (!botItemIdList.contains(BotItemDTO.FISH_TOOL)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemDTO.FISH_TOOL));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼竿一把(-90)，谢谢惠顾。\n"));
		}

		if (!botItemIdList.contains(BotItemDTO.FISH_FOOD)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemDTO.FISH_FOOD).setItemNum(10));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼饵10份(-10)，谢谢惠顾。\n"));
		}

		Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), fishPlayer.getStatus(), FishPlayerConstant.STATUS_FISHING);
		Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
		fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setStartTime(new Date()));
		botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(BotItemDTO.FISH_FOOD).setNum(-1));
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

}
