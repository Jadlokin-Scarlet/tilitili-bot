package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.FishPlayerConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.query.FishConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.*;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
	private final Random random;

	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final FishConfigMapper fishConfigMapper;
	private final BotItemMapper botItemMapper;
	private final BotUserMapper botUserMapper;
	private final BotUserManager botUserManager;

	@Autowired
	public PlayFishGameHandle(FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager, FishConfigMapper fishConfigMapper, BotItemMapper botItemMapper, BotUserMapper botUserMapper, BotUserManager botUserManager) {
//		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.fishConfigMapper = fishConfigMapper;
		this.botItemMapper = botItemMapper;
		this.botUserMapper = botUserMapper;
		this.botUserManager = botUserManager;

		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "抛竿": case "抛杆": return handleStart(messageAction);
			case "收竿": case "收杆": return handleEnd(messageAction);
			case "鱼呢": return getStatus(messageAction);
			case "钓鱼榜": return getRank(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage getRank(BotMessageAction messageAction) {
		Date todayStartTime = DateUtils.getCurrentDay();
		Date yesterdayStartTime = DateUtils.addDay(todayStartTime, -1);
		List<FishPlayer> fishPlayerList = fishPlayerMapper.listFishPlayerByEndTime(yesterdayStartTime, todayStartTime);
//		List<SortObject<Long>> userScoreList = new ArrayList<>();
		Map<Long, Integer> userScoreMap = new HashMap<>();
		for (FishPlayer fishPlayer : fishPlayerList) {
			if (fishPlayer.getItemId() == null) continue;
			FishConfig fishConfig = fishConfigMapper.getFishConfigById(fishPlayer.getItemId());
			if (fishConfig.getPrice() != null) {
				userScoreMap.merge(fishPlayer.getUserId(), fishConfig.getPrice(), Integer::sum);
			}
			if (fishConfig.getItemId() != null) {
				BotItem botItem = botItemMapper.getBotItemById(fishConfig.getItemId());
				if (botItem.getSellPrice() != null) {
					userScoreMap.merge(fishPlayer.getUserId(), botItem.getSellPrice(), Integer::sum);
				}
			}
		}
		List<String> rankList = userScoreMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(5)
				.map((Map.Entry<Long, Integer> entry) -> String.format("%s\t%s", botUserManager.getBotUserByIdWithParent(entry.getKey()).getName(), entry.getValue()))
				.collect(Collectors.toList());

		return BotMessage.simpleTextMessage(IntStream.range(0, rankList.size()).mapToObj(index -> String.format("%s:%s", index==0?"钓鱼佬":index+1, rankList.get(index))).collect(Collectors.joining("\n")));
	}

	private BotMessage getStatus(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.notNull(fishPlayer, "你还没抛竿");
		if (FishPlayerConstant.STATUS_FISHING.equals(fishPlayer.getStatus())) {
			if (fishPlayer.getNotifyTime() == null) {
				return BotMessage.simpleTextMessage("鱼还没来呢。");
			} else {
				return BotMessage.simpleTextMessage("鱼已经跑啦");
			}
		} else if (FishPlayerConstant.STATUS_COLLECT.equals(fishPlayer.getStatus())) {
			return BotMessage.simpleTextMessage("鱼上钩了，快收杆！");
		} else {
			throw new AssertException();
		}
	}

	private BotMessage handleEnd(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.notNull(fishPlayer, "你还没抛竿");
		if (FishPlayerConstant.STATUS_FISHING.equals(fishPlayer.getStatus())) {
			Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), FishPlayerConstant.STATUS_FISHING, FishPlayerConstant.STATUS_FINALL);
			Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
			BotItem botItem = botItemMapper.getBotItemById(BotItemDTO.FISH_FOOD);
			if (fishPlayer.getNotifyTime() == null) {
				botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(1));
				return BotMessage.simpleTextMessage("啥也没有。。");
			} else {
				return BotMessage.simpleTextMessage(String.format("似乎来晚了。。(%s-1)", botItem.getName()));
			}
		}
		Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), FishPlayerConstant.STATUS_COLLECT, FishPlayerConstant.STATUS_FINALL);
		Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
		Integer scale = fishPlayer.getScale();
		List<FishConfig> configList = fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setScale(scale).setStatus(0));
		int rateSum = configList.stream().mapToInt(FishConfig::getRate).sum();
		int theRate = random.nextInt(rateSum);
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
		String icon;
		if (itemId != null) {
			BotItem botItem = botItemMapper.getBotItemById(itemId);
			resultList.add(BotMessageChain.ofPlain(String.format("钓到一个%s，%s", botItem.getName(), botItem.getDescription())));
			botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(itemId).setNum(1));
			resultList.add(BotMessageChain.ofPlain(String.format("(%s+1)", botItem.getName())));
			icon = botItem.getIcon();
		} else {
			resultList.add(BotMessageChain.ofPlain(description));
			icon = fishConfig.getIcon();
		}

		if (price != null && price > 0) {
			botUserManager.safeUpdateScore(botUser, price);
			resultList.add(BotMessageChain.ofPlain(String.format("(积分+%s)", price)));
		}

		if (cost != null) {
			BotItem botItem = botItemMapper.getBotItemById(BotItemDTO.FISH_FOOD);
			Integer subNum = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(1 - cost));
			// 钓鱼的饵提前消耗掉过一个
			subNum--;
			if (subNum != 0) {
				resultList.add(BotMessageChain.ofPlain(String.format("(%s%s%s)", botItem.getName(), subNum > 0? "+": "-", Math.abs(subNum))));
			}
		}

		if (icon != null) {
			resultList.add(BotMessageChain.ofPlain("\n"));
			resultList.add(BotMessageChain.ofImage(icon));
		}

		return BotMessage.simpleListMessage(resultList);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.isTrue(fishPlayer == null || FishPlayerConstant.STATUS_WAIT.equals(fishPlayer.getStatus()), "你已经在钓啦！");
		if (fishPlayer == null) {
			fishPlayer = new FishPlayer();
			fishPlayer.setUserId(userId);
			fishPlayer.setStatus(FishPlayerConstant.STATUS_WAIT);
			fishPlayerMapper.addFishPlayerSelective(fishPlayer);
		}
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
		fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setStartTime(new Date()).setSenderId(senderId));
		botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(BotItemDTO.FISH_FOOD).setNum(-1));
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

}
