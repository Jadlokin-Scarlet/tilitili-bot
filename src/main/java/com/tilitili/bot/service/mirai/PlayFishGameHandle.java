package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.constant.BotPlaceConstant;
import com.tilitili.common.constant.BotUserConstant;
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
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class PlayFishGameHandle extends ExceptionRespMessageToSenderHandle {
	private final Random random;

	private final FishPlayerMapper fishPlayerMapper;
	private final BotUserItemMappingMapper botUserItemMappingMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final FishConfigMapper fishConfigMapper;
	private final BotItemMapper botItemMapper;
	private final BotUserManager botUserManager;
	private final BotPlaceMapper botPlaceMapper;
	private final BotUserMapMappingMapper botUserMapMappingMapper;
	private final BotUserConfigMapper botUserConfigMapper;
	private final RedisCache redisCache;
	private final PlayFishGameNewHandle playFishGameNewHandle;

	private List<Long> testBotList = new ArrayList<>();

	@Value("${PlayFishGameHandle.testBotList:}")
	public void setTestBotList(String testBotList) {
		try {
			this.testBotList = Arrays.stream(testBotList.split(",")).filter(StringUtils::isNotBlank).map(Long::parseLong).collect(Collectors.toList());
			log.info("刷新testBotList成功，{}", this.testBotList);
		} catch (Exception e) {
			log.error("刷新testBotList异常", e);
		}
	}


	@Autowired
	public PlayFishGameHandle(FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager, FishConfigMapper fishConfigMapper, BotItemMapper botItemMapper, BotUserManager botUserManager, BotPlaceMapper botPlaceMapper, BotUserMapMappingMapper botUserMapMappingMapper, BotUserConfigMapper botUserConfigMapper, RedisCache redisCache, PlayFishGameNewHandle playFishGameNewHandle) {
//		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.fishConfigMapper = fishConfigMapper;
		this.botItemMapper = botItemMapper;
		this.botUserManager = botUserManager;
		this.botPlaceMapper = botPlaceMapper;
		this.botUserMapMappingMapper = botUserMapMappingMapper;
		this.botUserConfigMapper = botUserConfigMapper;
		this.redisCache = redisCache;
		this.playFishGameNewHandle = playFishGameNewHandle;

		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	protected List<BotMessage> mockMessageInWaiteSender(BotMessageAction messageAction) {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "抛竿": case "抛杆": {
				return Collections.singletonList(BotMessage.simpleTextMessage("抛竿成功，有动静我会再叫你哦。"));
			}
			case "收竿": case "收杆": {
				return Collections.singletonList(BotMessage.simpleTextMessage("啥也没有。。"));
			}
			default: throw new AssertException();
		}
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		// 灰度
		if (testBotList.contains(messageAction.getBot().getId())) {
			return playFishGameNewHandle.handleMessage(messageAction);
		}
		switch (messageAction.getKeyWithoutPrefix()) {
			case "抛竿": case "抛杆": return handleStart(messageAction);
			case "收竿": case "收杆": return handleEnd(messageAction);
			case "鱼呢": return getStatus(messageAction);
			case "钓鱼榜": return getRank(messageAction);
			case "乐观榜": return getRateRank(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage getRateRank(BotMessageAction messageAction) {
		Date todayStartTime = DateUtils.getCurrentDay();
		Date yesterdayStartTime = DateUtils.addDay(todayStartTime, -1);
		List<FishPlayer> fishPlayerList = fishPlayerMapper.listFishPlayerByEndTime(yesterdayStartTime, todayStartTime);
		Map<Long, Integer> userScoreMap = new HashMap<>();
		Map<Long, Integer> userCntMap = new HashMap<>();
		Map<Long, Integer> userRateMap = new HashMap<>();
		for (FishPlayer fishPlayer : fishPlayerList) {
			Long userId = fishPlayer.getUserId();
			if (fishPlayer.getItemId() == null) continue;
			FishConfig fishConfig = fishConfigMapper.getFishConfigById(fishPlayer.getItemId());
			if (fishConfig == null) continue;
			userCntMap.merge(userId, 1, Integer::sum);
			if (fishConfig.getPrice() != null) {
				userScoreMap.merge(userId, fishConfig.getPrice(), Integer::sum);
			}
			if (fishConfig.getItemId() != null) {
				BotItem botItem = botItemMapper.getBotItemById(fishConfig.getItemId());
				if (botItem.getSellPrice() != null) {
					userScoreMap.merge(userId, botItem.getSellPrice(), Integer::sum);
				}
			}
		}
		userScoreMap.forEach((userId, score) -> {
			if (userCntMap.containsKey(userId)) {
				userRateMap.put(userId, score / userCntMap.get(userId));
			}
		});
		List<String> rankList = userRateMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(5)
				.map((Map.Entry<Long, Integer> entry) -> String.format("%s\t%s\t%s", userCntMap.get(entry.getKey()), userScoreMap.get(entry.getKey()), botUserManager.getValidBotUserByIdWithParent(entry.getKey()).getName()))
				.collect(Collectors.toList());

		return BotMessage.simpleTextMessage("次数\t积分\t酋长\n" + String.join("\n", rankList));
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
			if (fishConfig == null) continue;
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
				.map((Map.Entry<Long, Integer> entry) -> String.format("%s\t%s", entry.getValue(), botUserManager.getValidBotUserByIdWithParent(entry.getKey()).getName()))
				.collect(Collectors.toList());

		return BotMessage.simpleTextMessage(IntStream.range(0, rankList.size()).mapToObj(index -> String.format("%s:%s", index==0?"钓鱼佬":index+1, rankList.get(index))).collect(Collectors.joining("\n")));
	}

	private BotMessage getStatus(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.notNull(fishPlayer, "你还没抛竿");
		Asserts.checkEquals(fishPlayer.getVersion(), FishPlayerConstant.VERSION_OLD, "版本不兼容");
		if (FishPlayerConstant.STATUS_FISHING.equals(fishPlayer.getStatus())) {
			return BotMessage.simpleTextMessage("鱼还没来呢。");
		} else if (FishPlayerConstant.STATUS_COLLECT.equals(fishPlayer.getStatus())) {
			return BotMessage.simpleTextMessage("鱼上钩了，快收杆！");
		} else if (FishPlayerConstant.STATUS_FAIL.equals(fishPlayer.getStatus())) {
			return BotMessage.simpleTextMessage("鱼已经跑啦");
		} else {
			throw new AssertException();
		}
	}

	private BotMessage handleEnd(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		FishPlayer fishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		Asserts.notNull(fishPlayer, "你还没抛竿");
		Asserts.checkEquals(fishPlayer.getVersion(), FishPlayerConstant.VERSION_OLD, "版本不兼容");

		if (!FishPlayerConstant.STATUS_COLLECT.equals(fishPlayer.getStatus())) {
			Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), fishPlayer.getStatus(), FishPlayerConstant.STATUS_FINALL);
			Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
			BotItem botItem = botItemMapper.getBotItemById(BotItemConstant.FISH_FOOD);
			if (FishPlayerConstant.STATUS_FISHING.equals(fishPlayer.getStatus())) {
				botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(botItem.getId()).setNum(1));
				return BotMessage.simpleTextMessage("啥也没有。。");
			} else if (FishPlayerConstant.STATUS_FAIL.equals(fishPlayer.getStatus())) {
				return BotMessage.simpleTextMessage(String.format("似乎来晚了。。(%s-1)", botItem.getName()));
			} else {
				throw new AssertException();
			}
		}
		Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), FishPlayerConstant.STATUS_COLLECT, FishPlayerConstant.STATUS_FINALL);
		Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
		List<FishConfig> configList = fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setPlaceId(fishPlayer.getPlaceId()).setStatus(0));
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

			boolean hasItem = botUserItemMappingManager.hasItem(botUser.getId(), botItem.getId());
			// 自动回收
			boolean autoSellFish = "yes".equals(botUserConfigMapper.getValueByUserIdAndKey(botUser.getId(), ConfigHandle.autoSellFishKey));
			// 回收重复
			boolean autoSellRepeatFish = hasItem && "yes".equals(botUserConfigMapper.getValueByUserIdAndKey(botUser.getId(), ConfigHandle.autoSellRepeatFishKey));
			// 只回收不大于2000的
			boolean notPrecious = botItem.getSellPrice() <= 2000;
			if ((autoSellFish || autoSellRepeatFish) && notPrecious) {
				Integer updScore = botUserManager.safeUpdateScore(botUser, botItem.getSellPrice());
				resultList.add(BotMessageChain.ofPlain(String.format("(%+d分)", updScore)));
			} else {
				botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(itemId).setNum(1));
				resultList.add(BotMessageChain.ofPlain(String.format("(%s+1)", botItem.getName())));
			}
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
			BotItem botItem = botItemMapper.getBotItemById(BotItemConstant.FISH_FOOD);
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

		try {
			BotMessage startMessage = this.handleStart(messageAction);
			resultList.add(BotMessageChain.ofPlain("\n"));
			resultList.addAll(startMessage.getBotMessageChainList());
		} catch (AssertException e) {
			resultList.add(BotMessageChain.ofPlain("自动抛竿失败，" + e.getMessage()));
		}
		return BotMessage.simpleListMessage(resultList);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();

		List<BotMessageChain> resultList = new ArrayList<>();

		List<BotItemDTO> botItemList = botUserItemMappingMapper.getItemListByUserId(userId);
		List<Long> botItemIdList = botItemList.stream().map(BotItemDTO::getItemId).collect(Collectors.toList());

		// 兑换道具
		if (!botItemIdList.contains(BotItemConstant.FISH_TOOL)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemConstant.FISH_TOOL));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼竿一把(-90)，谢谢惠顾。\n"));
			if (botUser.getType() != BotUserConstant.USER_TYPE_QQ) {
				resultList.add(BotMessageChain.ofPlain("tips: 有共同群聊最好先申请合体再玩。\n"));
			}
		}

		if (!botItemIdList.contains(BotItemConstant.FISH_FOOD)) {
			botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemConstant.FISH_FOOD).setItemNum(10));
			resultList.add(BotMessageChain.ofPlain("为您自动兑换鱼饵10份(-10)，谢谢惠顾。\n"));
		}

		Asserts.checkNull(fishPlayerMapper.getValidFishPlayerByUserId(userId), "你已经在钓啦！");
		BotUserMapMapping userMapMapping = botUserMapMappingMapper.getBotUserMapMappingByUserId(userId);
		Long placeId = userMapMapping == null? BotPlaceConstant.PLACE_FIRST_FISH: userMapMapping.getPlaceId();
		List<FishConfig> placeFishConfig = fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setPlaceId(placeId));
		Asserts.notEmpty(placeFishConfig, "这里没有鱼可以钓。。");

		FishPlayer fishPlayer = new FishPlayer();
		fishPlayer.setUserId(userId);
		fishPlayer.setPlaceId(placeId);
		fishPlayer.setStatus(FishPlayerConstant.STATUS_FISHING);
		fishPlayer.setStartTime(new Date());
		fishPlayer.setSenderId(senderId);
		fishPlayerMapper.addFishPlayerSelective(fishPlayer);

		botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(BotItemConstant.FISH_FOOD).setNum(-1));
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

}
