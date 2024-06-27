package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.component.CloseableRedisLock;
import com.tilitili.common.constant.*;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotItemDTO;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.query.FishConfigQuery;
import com.tilitili.common.entity.query.FishPlayerQuery;
import com.tilitili.common.entity.query.FishPlayerTouchQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotConfigManager;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
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
	private final BotUserMapMappingMapper botUserMapMappingMapper;
	private final BotConfigManager botConfigManager;
	private final PlayFishGameNewHandle playFishGameNewHandle;
	private final RedisCache redisCache;
	private final FishPlayerTouchMapper fishPlayerTouchMapper;

	private static final String FISH_LOCK_KEY = "fishLock-";
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
	public PlayFishGameHandle(FishPlayerMapper fishPlayerMapper, BotUserItemMappingMapper botUserItemMappingMapper, BotUserItemMappingManager botUserItemMappingManager, FishConfigMapper fishConfigMapper, BotItemMapper botItemMapper, BotUserManager botUserManager, BotUserMapMappingMapper botUserMapMappingMapper, BotConfigManager botConfigManager, PlayFishGameNewHandle playFishGameNewHandle, RedisCache redisCache, FishPlayerTouchMapper fishPlayerTouchMapper) {
//		this.fishGame = fishGame;
		this.fishPlayerMapper = fishPlayerMapper;
		this.botUserItemMappingMapper = botUserItemMappingMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.fishConfigMapper = fishConfigMapper;
		this.botItemMapper = botItemMapper;
		this.botUserManager = botUserManager;
		this.botUserMapMappingMapper = botUserMapMappingMapper;
		this.botConfigManager = botConfigManager;
		this.playFishGameNewHandle = playFishGameNewHandle;
		this.redisCache = redisCache;
		this.fishPlayerTouchMapper = fishPlayerTouchMapper;

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
			case "摸鱼": return handleTouch(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleTouch(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		BotUserDTO touchUser = messageAction.getBotUser();
		List<Long> fishIdList = fishPlayerMapper.getFishPlayerByCondition(new FishPlayerQuery()
				.setSenderId(senderId).setStatus(FishPlayerConstant.STATUS_COLLECT)
		).stream().map(FishPlayer::getId).collect(Collectors.toList());
		
		List<String> successResult = new ArrayList<>();
		for (Long fishId : fishIdList) {
			try (CloseableRedisLock ignored = new CloseableRedisLock(redisCache, FISH_LOCK_KEY+fishId)) {
				FishPlayer fishPlayer = fishPlayerMapper.getFishPlayerById(fishId);
				successResult.add(this.touchFish(touchUser, fishPlayer));
			} catch (AssertException ignore) {}
		}
		
		if (!successResult.isEmpty()) {
			return BotMessage.simpleTextMessage(String.join("\n", successResult));
		}

		List<FishPlayer> fishPlayerList = fishPlayerMapper.getFishPlayerByCondition(new FishPlayerQuery()
				.setSenderId(senderId)
				.setPageSize(1).setPageNo(1)
				.setSorter("notify_time").setSorted("desc")
		);
		Asserts.notEmpty(fishPlayerList, "鱼呢");
		FishPlayer fishPlayer = fishPlayerList.get(0);
		return BotMessage.simpleTextMessage(this.touchFish(touchUser, fishPlayer));
	}

	private String touchFish(BotUserDTO touchUser, FishPlayer fishPlayer) {
		Long touchUserId = touchUser.getId();
		Asserts.notEquals(fishPlayer.getStatus(), FishPlayerConstant.STATUS_FAIL, "鱼已经跑啦");
		Asserts.checkEquals(fishPlayer.getStatus(), FishPlayerConstant.STATUS_COLLECT, "鱼呢");
		Asserts.notEquals(fishPlayer.getUserId(), touchUserId, "自己的鱼也摸？");
		Asserts.notNull(fishPlayer.getItemId());

		BotUserDTO theUserBeingTouched = botUserManager.getValidBotUserByIdWithParent(fishPlayer.getUserId());

		FishConfig fishConfig = fishConfigMapper.getFishConfigById(fishPlayer.getItemId());
		Asserts.notNull(fishConfig);
		Long itemId = fishConfig.getItemId();
		Asserts.notNull(fishConfig.getItemId(), "没鱼可摸喵");
		BotItem botItem = botItemMapper.getBotItemById(itemId);
		Asserts.notNull(botItem);
		Integer totalValue = botItem.getSellPrice();
		int usableValue = totalValue * 4 / 10;

		boolean precious = isPrecious(botItem);
		Asserts.isFalse(precious, "没鱼可摸喵");

		List<FishPlayerTouch> touchList = fishPlayerTouchMapper.getFishPlayerTouchByCondition(new FishPlayerTouchQuery().setFishId(fishPlayer.getId()));
		boolean touched = touchList.stream().map(FishPlayerTouch::getTouchUserId).anyMatch(Predicate.isEqual(touchUserId));
		Asserts.isFalse(touched, "已经摸过啦，收下留情啊");

		int usedValue = touchList.stream().mapToInt(FishPlayerTouch::getValue).sum();
		int remainderValue = usableValue - usedValue;
		Asserts.isTrue(remainderValue > 0, "手下留情啊，给ta留点吧！");

		int theValue = ThreadLocalRandom.current().nextInt(remainderValue) + 1;
		fishPlayerTouchMapper.addFishPlayerTouchSelective(new FishPlayerTouch().setFishId(fishPlayer.getId()).setTouchUserId(touchUserId).setValue(theValue));

		Integer updScore = botUserManager.safeUpdateScore(touchUser, theValue);

		String theValueRateStr = ThreadConstant.format2f.get().format(theValue * 100.0 / totalValue) + "%";
		return String.format("摸走%s %s！(+%d分)", theUserBeingTouched.getName(), theValueRateStr, updScore);
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
			if (fishPlayer.getCollectTime() == null) continue;
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
				.map((Map.Entry<Long, Integer> entry) -> String.format("%s %s %s", userCntMap.get(entry.getKey()), userScoreMap.get(entry.getKey()), botUserManager.getValidBotUserByIdWithParent(entry.getKey()).getName()))
				.collect(Collectors.toList());

		return BotMessage.simpleTextMessage("次数 积分 酋长 " + String.join("\n", rankList));
	}

	private BotMessage getRank(BotMessageAction messageAction) {
		Date todayStartTime = DateUtils.getCurrentDay();
		Date yesterdayStartTime = DateUtils.addDay(todayStartTime, -1);
		List<FishPlayer> fishPlayerList = fishPlayerMapper.listFishPlayerByEndTime(yesterdayStartTime, todayStartTime);
//		List<SortObject<Long>> userScoreList = new ArrayList<>();
		Map<Long, Integer> userScoreMap = new HashMap<>();
		for (FishPlayer fishPlayer : fishPlayerList) {
			if (fishPlayer.getItemId() == null) continue;
			if (fishPlayer.getCollectTime() == null) continue;
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
			List<FishPlayerTouch> touchList = fishPlayerTouchMapper.getFishPlayerTouchByCondition(new FishPlayerTouchQuery().setFishId(fishPlayer.getId()));
			for (FishPlayerTouch touch : touchList) {
				userScoreMap.merge(fishPlayer.getUserId(), -touch.getValue(), Integer::sum);
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
			List<FishPlayerTouch> touchList = fishPlayerTouchMapper.getFishPlayerTouchByCondition(new FishPlayerTouchQuery().setFishId(fishPlayer.getId()));
			String remainderRateStr;
			if (touchList.isEmpty()) {
				remainderRateStr = "";
			} else {
				Asserts.notNull(fishPlayer.getItemId());
				FishConfig fishConfig = fishConfigMapper.getFishConfigById(fishPlayer.getItemId());
				Asserts.notNull(fishConfig.getItemId());
				BotItem botItem = botItemMapper.getBotItemById(fishConfig.getItemId());
				Integer totalValue = botItem.getSellPrice();

				int usedValue = touchList.stream().mapToInt(FishPlayerTouch::getValue).sum();
				remainderRateStr = String.format("(剩余%.2f%%)", (totalValue - usedValue) * 100.0 / totalValue);
			}
			return BotMessage.simpleTextMessage("鱼上钩了，快收杆！"+remainderRateStr);
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
		try (CloseableRedisLock ignored = new CloseableRedisLock(redisCache, FISH_LOCK_KEY +fishPlayer.getId())) {
			if (!FishPlayerConstant.STATUS_COLLECT.equals(fishPlayer.getStatus())) {
				Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), fishPlayer.getStatus(), FishPlayerConstant.STATUS_FINAL);
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
			Integer updCnt = fishPlayerMapper.safeUpdateStatus(fishPlayer.getId(), FishPlayerConstant.STATUS_COLLECT, FishPlayerConstant.STATUS_FINAL);
			Asserts.checkEquals(updCnt, 1, "啊嘞，不对劲");
			fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setCollectTime(new Date()));

			FishConfig fishConfig;
			if (fishPlayer.getItemId() == null) {
				fishConfig = this.randomFishConfig(fishPlayer.getPlaceId());
				Asserts.notNull(fishConfig, "啊嘞，不对劲");
				fishPlayerMapper.updateFishPlayerSelective(new FishPlayer().setId(fishPlayer.getId()).setItemId(fishConfig.getId()));
			} else {
				fishConfig = fishConfigMapper.getFishConfigById(fishPlayer.getItemId());
				Asserts.notNull(fishConfig, "啊嘞，不对劲");
			}


			String description = fishConfig.getDescription();
			Long itemId = fishConfig.getItemId();
			Integer price = fishConfig.getPrice();
			Integer cost = fishConfig.getCost();

			List<BotMessageChain> resultList = new ArrayList<>();
			String icon;
			if (itemId != null) {
				BotItem botItem = botItemMapper.getBotItemById(itemId);
				resultList.add(BotMessageChain.ofPlain(String.format("钓到一个%s，%s", botItem.getName(), botItem.getDescription())));

				boolean hasItem = botUserItemMappingManager.hasItem(userId, botItem.getId());
				// 自动回收
				boolean autoSellFish = Boolean.TRUE.equals(botConfigManager.getBooleanUserConfigCache(userId, ConfigHandle.autoSellFishKey));
				// 回收重复
				boolean autoSellRepeatFish = hasItem && Boolean.TRUE.equals(botConfigManager.getBooleanUserConfigCache(userId, ConfigHandle.autoSellRepeatFishKey));
				// 过于贵重的不会回收
				boolean notPrecious = !this.isPrecious(botItem);
				// 是否被摸过，摸过只能回收积分
				List<FishPlayerTouch> touchList = fishPlayerTouchMapper.getFishPlayerTouchByCondition(new FishPlayerTouchQuery().setFishId(fishPlayer.getId()));
				boolean touched = !touchList.isEmpty();

				if (touched || ((autoSellFish || autoSellRepeatFish) && notPrecious)) {
					Integer totalValue = botItem.getSellPrice();
					int usedValue = touchList.stream().mapToInt(FishPlayerTouch::getValue).sum();
					int remainderValue = totalValue - usedValue;
					Integer updScore = botUserManager.safeUpdateScore(botUser, remainderValue);
					resultList.add(BotMessageChain.ofPlain(String.format("(%+d分)", updScore)));
					if (touched) {
						resultList.add(BotMessageChain.ofPlain(String.format("(%.2f%%)", remainderValue * 100.0 / totalValue)));
					}
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
					resultList.add(BotMessageChain.ofPlain(String.format("(%s%s%s)", botItem.getName(), subNum > 0 ? "+" : "-", Math.abs(subNum))));
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
	}

	private boolean isPrecious(BotItem botItem) {
		return botItem.getSellPrice() > 2000;
	}

	private FishConfig randomFishConfig(Long placeId) {
		FishConfig fishConfig = null;
		List<FishConfig> configList = fishConfigMapper.getFishConfigByCondition(new FishConfigQuery().setPlaceId(placeId).setStatus(0));
		int rateSum = configList.stream().mapToInt(FishConfig::getRate).sum();
		int theRate = random.nextInt(rateSum);
		for (FishConfig config : configList) {
			theRate -= config.getRate();
			if (theRate <= 0) {
				fishConfig = config;
				break;
			}
		}
		return fishConfig;
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

		FishPlayer oldFishPlayer = fishPlayerMapper.getValidFishPlayerByUserId(userId);
		if (oldFishPlayer != null) {
			Asserts.checkEquals(oldFishPlayer.getStatus(), FishPlayerConstant.STATUS_FAIL, "你已经在钓啦！");
			BotMessage endMessage = this.handleEnd(messageAction);
			resultList.add(BotMessageChain.ofPlain("自动收竿："));
			resultList.addAll(endMessage.getBotMessageChainList());
			resultList.add(BotMessageChain.ofPlain("\n"));
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

		FishConfig fishConfig = this.randomFishConfig(placeId);
		fishPlayer.setItemId(fishConfig.getId());
		fishPlayerMapper.addFishPlayerSelective(fishPlayer);

		botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(BotItemConstant.FISH_FOOD).setNum(-1));
		resultList.add(BotMessageChain.ofPlain("抛竿成功，有动静我会再叫你哦。"));
		return BotMessage.simpleListMessage(resultList);
	}

}
