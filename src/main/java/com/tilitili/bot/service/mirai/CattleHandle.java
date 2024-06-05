package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotItemService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotItemConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.SafeTransactionDTO;
import com.tilitili.common.entity.query.BotCattleQuery;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotCattleManager;
import com.tilitili.common.manager.BotUserItemMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotCattleMapper;
import com.tilitili.common.mapper.mysql.BotCattleRecordMapper;
import com.tilitili.common.mapper.mysql.BotItemMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class CattleHandle extends ExceptionRespMessageToSenderHandle {
	private static final String cattleSleepKey = "CattleHandle-";
	private static final String cattleApplyKey = "CattleHandle-applyPk-";
	private static final String cattleApplyNowKey = "CattleHandle-nowPk-";

	private final RedisCache redisCache;
	private final BotItemMapper botItemMapper;
	private final BotUserManager botUserManager;
	private final BotItemService botItemService;
	private final BotCattleMapper botCattleMapper;
	private final BotCattleManager botCattleManager;
	private final BotCattleRecordMapper botCattleRecordMapper;
	private final BotUserItemMappingManager botUserItemMappingManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	private final Random random;

	@Autowired
	public CattleHandle(BotCattleMapper botCattleMapper, BotCattleManager botCattleManager, RedisCache redisCache, BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper, BotCattleRecordMapper botCattleRecordMapper, BotUserItemMappingManager botUserItemMappingManager, BotItemService botItemService, BotItemMapper botItemMapper) {
		this.redisCache = redisCache;
		this.botItemMapper = botItemMapper;
		this.botItemService = botItemService;
		this.botUserManager = botUserManager;
		this.botCattleMapper = botCattleMapper;
		this.botCattleManager = botCattleManager;
		this.botCattleRecordMapper = botCattleRecordMapper;
		this.botUserItemMappingManager = botUserItemMappingManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "我的牛子": case "我的牛牛": return handleInfo(messageAction);
			case "领取牛子": case "领取牛牛": return handleStart(messageAction);
			case "比划比划": return handlePk(messageAction);
			case "天榜": return handleRank(messageAction);
			case "地榜": return handleDescRank(messageAction);
			case "赎回牛子": case "赎回牛牛": return handleRedemption(messageAction);
			case "我牛子呢": case "我牛牛呢": return handleRecord(messageAction);
			case "牛子榜": return BotMessage.simpleTextMessage("你是否在找：天榜");
			case "牛子0榜": return BotMessage.simpleTextMessage("你是否在找：地榜");
			case "决斗": case "撅斗": return handleApplyPK(messageAction);
			case "杀": return handleAcceptPK(messageAction);
			case "男蛮入侵": return handleAOE(messageAction);
			case "鸣金收兵": return handleStop(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleStop(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		BotCattle botCattle = botCattleMapper.getValidBotCattleByUserId(botUser.getId());
		if (botCattle == null) {
			return null;
		}
		Asserts.notEquals(botCattle.getStatus(), -1, "找茬是⑧");
		if (botCattle.getStartTime() != null) {
			Date limitDate = DateUtils.addTime(new Date(), Calendar.DAY_OF_YEAR, -1);
			Asserts.isTrue(botCattle.getStartTime().before(limitDate), "现在还不能休息哦❤");
		}
		int cnt = botCattleMapper.updateBotCattleSelective(new BotCattle().setId(botCattle.getId()).setStatus(-1));
		Asserts.checkEquals(cnt, 1, "啊嘞，不对劲");

		return BotMessage.simpleTextMessage("撤！");
	}

	private BotMessage handleAOE(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String redisKey = cattleSleepKey + userId;
		Long expire = redisCache.getExpire(redisKey);

		BotItem refreshItem = botItemMapper.getBotItemById(BotItemConstant.CATTLE_REFRESH);

//		int max = 5;
//		int selfCnt = expire > 0? 0: 1;
//		int itemCnt = Math.min(max - selfCnt, botUserItemMappingManager.countItem(botUser, BotItemConstant.CATTLE_REFRESH));
//		int buyCnt = Math.min(max - selfCnt - itemCnt, botUser.getScore() / refreshItem.getPrice());
//		int pkCnt = selfCnt + itemCnt + buyCnt;
//		Asserts.isTrue(pkCnt > 0, "积分好像不够惹。休息%s再来吧", expire > 60 ? expire / 60 + "分钟" : expire + "秒");


//		if (expire > 0) {
//			Asserts.isTrue(botUserItemMappingManager.hasItem(userId, BotItemConstant.CATTLE_REFRESH), "节制啊，再休息%s吧", expire > 60 ? expire / 60 + "分钟" : expire + "秒");
//		}

		BotCattle cattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(cattle, "巧妇难为无米炊。");

		List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		List<BotCattle> senderCattleList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId)
				.filter(Predicate.isEqual(userId).negate())
				.map(otherUserId -> botUserManager.getValidBotUserByIdWithParent(botSender.getId(), otherUserId))
				.filter(Objects::nonNull)
				.map(BotUserDTO::getId)
				.filter(otherUserId -> redisCache.getExpire(cattleSleepKey +otherUserId) <= 0)
				.map(botCattleMapper::getValidBotCattleByUserId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		Asserts.notEmpty(senderCattleList, "拔剑四顾心茫然。");
		Collections.shuffle(senderCattleList);

		List<BotMessageChain> respList = new ArrayList<>();
		int pkCnt = 0;
		int buyCnt = 0;
		int useCnt = 0;
		for (int i = 0; i < Math.min(5, senderCattleList.size()); i++) {
			boolean hasCd = redisCache.getExpire(redisKey) > 0;
			if (hasCd) {
				boolean hasItem = botUserItemMappingManager.hasItem(userId, BotItemConstant.CATTLE_REFRESH);
				if (!hasItem) {
					try {
						botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(userId).setItemId(BotItemConstant.CATTLE_REFRESH));
						buyCnt++;
					} catch (AssertException e) {
						break;
					}
				}
				Asserts.isTrue(botItemService.useItem(botSender, botUser, refreshItem), "啊嘞，不对劲");
				useCnt++;
			}

			BotCattle otherCattle = senderCattleList.get(i);
			Long otherUserId = otherCattle.getUserId();
			String otherRedisKey = cattleSleepKey + otherUserId;

			if (i != 0) respList.add(BotMessageChain.ofPlain("\n"));
			respList.addAll(this.pk(botSender.getId(), userId, otherUserId, true));
			pkCnt++;

			redisCache.setValue(redisKey, "yes", 60*60);
			redisCache.setValue(otherRedisKey, "yes", 60*60);
		}
		Asserts.isTrue(pkCnt > 0, "积分好像不够惹。休息%s再来吧", expire > 60 ? expire / 60 + "分钟" : expire + "秒");
		if (useCnt > 0) {
			respList.add(0, BotMessageChain.ofPlain(String.format("连灌%s瓶伟哥，你感觉自己充满了力量%n", useCnt)));
		}
		if (buyCnt > 0) {
			String nowScore = String.valueOf(botUserManager.getValidBotUserByIdWithParent(botUser.getId()).getScore());
			respList.add(0, BotMessageChain.ofPlain(String.format("兑换%s个%s成功，剩余积分%s%n", buyCnt, refreshItem.getName(), nowScore)));
		}


//		int useCnt = 0;
//		for (int index = 0; index < Math.min(5, senderCattleList.size()); index++) {
//			boolean hasCd = redisCache.getExpire(redisKey) > 0;
//			boolean hasItem = botUserItemMappingManager.hasItem(userId, BotItemConstant.CATTLE_REFRESH);
//			if (hasCd && !hasItem) {
//				break;
//			}
//
//			if (hasCd) {
//				Asserts.isTrue(botItemService.useItem(botSender, botUser, refreshItem), "啊嘞，不对劲");
//				useCnt ++;
//			}
//
//			BotCattle otherCattle = senderCattleList.get(index);
//			Long otherUserId = otherCattle.getUserId();
//			String otherRedisKey = cattleSleepKey + otherUserId;
//
//			if (index != 0) respList.add(BotMessageChain.ofPlain("\n"));
//			respList.addAll(this.pk(botSender.getId(), userId, otherUserId, true));
//
//			redisCache.setValue(redisKey, "yes", 60*60);
//			redisCache.setValue(otherRedisKey, "yes", 60*60);
//		}
//		if (useCnt > 1) {
//			respList.add(0, BotMessageChain.ofPlain(String.format("连灌%s瓶伟哥，你感觉自己充满了力量%n", useCnt)));
//		} else if (useCnt == 1) {
//			respList.add(0, BotMessageChain.ofPlain(String.format("%s瓶伟哥，你感觉自己充满了力量%n", useCnt)));
//		}

		return BotMessage.simpleListMessage(respList).setQuote(messageId);
	}

//	private BotMessage handleAcceptPK(BotMessageAction messageAction) {
//		BotSender botSender = messageAction.getBotSender();
//		BotUserDTO theUser = messageAction.getBotUser();
//		Long theUserId = theUser.getId();
//
//		String theApplyRedisKey = String.format("CattleHandle-applyPk-%s-%s", botSender.getId(), theUserId);
//		if (!redisCache.exists(theApplyRedisKey)) {
//			return null;
//		}
//
//		long otherUserId = Long.parseLong((String) redisCache.getValue(theApplyRedisKey));
//		BotUserDTO otherUser = botUserManager.getBotUserByIdWithParent(botSender.getId(), otherUserId);
//
//		String otherApplyRedisKey = String.format("CattleHandle-applyPk-%s-%s", botSender.getId(), otherUserId);
//		String otherRedisKey = String.format("CattleHandle-%s", otherUserId);
//		Long otherExpire = redisCache.getExpire(otherRedisKey);
//		if (otherExpire > 0) {
//			Asserts.isTrue(botUserItemMappingManager.hasItem(otherUserId, BotItemConstant.CATTLE_REFRESH), "啊嘞，道具不够用了。");
//		}
//
//		String theRedisKey = String.format("CattleHandle-%s", theUserId);
//		Long theExpire = redisCache.getExpire(theRedisKey);
//		if (theExpire > 0) {
//			Asserts.isTrue(botUserItemMappingManager.hasItem(theUserId, BotItemConstant.CATTLE_REFRESH), "啊嘞，道具不够用了。");
//		}
//
//		BotItem refreshItem = botItemMapper.getBotItemById(BotItemConstant.CATTLE_REFRESH);
//
//		// 主逻辑
//		redisCache.delete(otherApplyRedisKey);
//		redisCache.delete(theApplyRedisKey);
//		if (otherExpire > 0) {
//			Asserts.isTrue(botItemService.useItemWithoutError(botSender, otherUser, refreshItem), "啊嘞，不对劲");
//		}
//
//		if (theExpire > 0) {
//			Asserts.isTrue(botItemService.useItemWithoutError(botSender, theUser, refreshItem), "啊嘞，不对劲");
//		}
//
//		List<BotMessageChain> resp = this.pk(botSender.getId(), theUserId, otherUserId, false);
//
//		redisCache.setValue(otherApplyRedisKey, String.valueOf(theUserId), 60);
//		redisCache.setValue(otherRedisKey, "yes", 60*60);
//		redisCache.setValue(theRedisKey, "yes", 60*60);
//		return BotMessage.simpleListMessage(resp);
//	}

	private BotMessage handleAcceptPK(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO theUser = messageAction.getBotUser();
		Long theUserId = theUser.getId();

		// 校验PK关系完整性，需要双向校验
		String theApplyRedisKey = cattleApplyKey + theUserId;
		if (!redisCache.exists(theApplyRedisKey)) {
			return null;
		}

		long otherUserId = redisCache.getValueLong(theApplyRedisKey);
		String otherApplyRedisKey = cattleApplyKey + otherUserId;
		String otherApplyNowRedisKey = cattleApplyNowKey + otherUserId;
		if (!redisCache.exists(otherApplyRedisKey)) {
			return null;
		}

		// 校验是否自己的回合，以及回合的有效性
		String theApplyNowRedisKey = cattleApplyNowKey + theUserId;
		if (!redisCache.exists(theApplyNowRedisKey)) {
			return null;
		}
		Long theApplyNowUserId = redisCache.getValueLong(theApplyNowRedisKey);
		if (!Objects.equals(theApplyNowUserId, otherUserId)) {
			return null;
		}

		// 校验双方牛子完整性
		BotCattle theCattle = botCattleMapper.getValidBotCattleByUserId(theUserId);
		BotCattle otherCattle = botCattleMapper.getValidBotCattleByUserId(otherUserId);
		Asserts.notNull(theCattle, "巧妇难为无米炊。");
		Asserts.notNull(otherCattle, "拔剑四顾心茫然。");

		List<BotMessageChain> respList = new ArrayList<>();
		// 发起者需要校验牛子可用性
		String theRedisKey = cattleSleepKey + theUserId;
		Long theExpire = redisCache.getExpire(theRedisKey);
		if (theExpire > 0) {
			boolean hasItem = botUserItemMappingManager.hasItem(theUserId, BotItemConstant.CATTLE_REFRESH);
			BotItem refreshItem = botItemMapper.getBotItemById(BotItemConstant.CATTLE_REFRESH);
			if (!hasItem) {
				botUserItemMappingManager.safeBuyItem(new SafeTransactionDTO().setUserId(theUserId).setItemId(BotItemConstant.CATTLE_REFRESH));
				String nowScore = String.valueOf(botUserManager.getValidBotUserByIdWithParent(theUserId).getScore());
				respList.add(BotMessageChain.ofPlain(String.format("兑换%s成功，剩余积分%s%n", refreshItem.getName(), nowScore)));
			}
			Asserts.isTrue(botItemService.useItemWithoutError(botSender, theUser, refreshItem), "啊嘞，不对劲");
		}

		int length = random.nextInt(1000);
		log.info(String.format("%s和%s比划", theUserId, otherUserId));

		botCattleManager.safeCalculateCattle(theUserId, otherUserId, length, -length);
		botCattleRecordMapper.addBotCattleRecordSelective(new BotCattleRecord().setSourceUserId(theUserId).setTargetUserId(otherUserId).setSourceLengthDiff(length).setTargetLengthDiff(-length).setResult(0).setLength(length));
		respList.add(BotMessageChain.ofPlain(String.format("一番胶战后，你赢得了%.2fcm，现在有%.2fcm。", length / 100.0, (theCattle.getLength() + length) / 100.0)));

		// 刷新pk关系时间
		redisCache.setValue(otherApplyRedisKey, theUserId, 60);
		redisCache.setValue(theApplyRedisKey, otherUserId, 60);
		// 发起者进入cd
		redisCache.setValue(theRedisKey, "yes", 60*60);
		// 攻守转换
		redisCache.delete(theApplyNowRedisKey);
		redisCache.setValue(otherApplyNowRedisKey, theUserId, 60);
		return BotMessage.simpleListMessage(respList);
	}

	private BotMessage handleApplyPK(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		BotCattle cattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(cattle, "巧妇难为无米炊。");

//		Asserts.isTrue(botUserItemMappingManager.hasItem(userId, BotItemConstant.CATTLE_REFRESH), "道具不够用惹");

		List<BotUserDTO> atList = messageAction.getAtList();
		Asserts.notEmpty(atList, "你要和谁决斗？");

		BotUserDTO otherUser = atList.get(0);
		Long otherUserId = otherUser.getId();
		Asserts.notEquals(userId, otherUserId, "你找茬是⑧");

		BotCattle otherCattle = botCattleMapper.getValidBotCattleByUserId(otherUserId);
		Asserts.notNull(otherCattle, "拔剑四顾心茫然。");

//		Asserts.isTrue(botUserItemMappingManager.hasItem(otherUserId, BotItemConstant.CATTLE_REFRESH), "对方道具不够用惹");

		// 记录PK关系
		String otherApplyRedisKey = cattleApplyKey + otherUserId;
		String applyRedisKey = cattleApplyKey + userId;
		redisCache.setValue(otherApplyRedisKey, userId, 60);
		redisCache.setValue(applyRedisKey, otherUserId, 60);
		// 记录下一轮的发起者
		redisCache.setValue(cattleApplyNowKey + otherUserId, userId, 60);

		return BotMessage.simpleTextMessage(String.format("%s发起击剑！快对他使用\"杀\"！", botUser.getName()));
	}

	private BotMessage handlePk(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String redisKey = cattleSleepKey + userId;
		Long expire = redisCache.getExpire(redisKey);
		Asserts.isTrue(expire <= 0, "节制啊，再休息%s吧", expire > 60 ? expire / 60 + "分钟" : expire + "秒");

		BotCattle cattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(cattle, "巧妇难为无米炊。");

		BotCattle otherCattle;
		boolean isRandom;
		List<BotUserDTO> atList = messageAction.getAtList();
		if (atList.isEmpty() && StringUtils.isNumber(messageAction.getValue())) {
			atList.add(botUserManager.getValidBotUserByExternalIdWithParent(botUser.getType(), messageAction.getValue()));
		}
		if (atList.isEmpty()) {
			if (StringUtils.isNotBlank(messageAction.getValue())) {
				return BotMessage.emptyMessage();
			}
			List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
			List<BotCattle> senderCattleList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId)
					.filter(Predicate.isEqual(userId).negate())
					.filter(otherUserId -> redisCache.getExpire(cattleSleepKey + otherUserId) <= 0)
					.map(botCattleMapper::getValidBotCattleByUserId)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (!senderCattleList.isEmpty()) {
				otherCattle = senderCattleList.get(random.nextInt(senderCattleList.size()));
			} else {
				otherCattle = null;
			}
			isRandom = true;
		} else {
			Long otherUserId = atList.get(0).getId();
			Asserts.notEquals(userId, otherUserId, "你找茬是⑧");
			otherCattle = botCattleMapper.getValidBotCattleByUserId(otherUserId);
			isRandom = false;
		}
		Asserts.notNull(otherCattle, "拔剑四顾心茫然。");

		Long otherUserId = otherCattle.getUserId();
		String otherRedisKey = cattleSleepKey + otherUserId;
		Long otherExpire = redisCache.getExpire(otherRedisKey);
		Asserts.isTrue(otherExpire <= 0, "让他再休息%s吧", otherExpire > 60? otherExpire/60+"分钟": otherExpire+"秒");

		List<BotMessageChain> respList = this.pk(botSender.getId(), userId, otherUserId, isRandom);

		redisCache.setValue(redisKey, "yes", 60*60);
		redisCache.setValue(otherRedisKey, "yes", 60*60);
		return BotMessage.simpleListMessage(respList).setQuote(messageId);
	}

	private List<BotMessageChain> pk(Long senderId, Long userId, Long otherUserId, boolean isRandom) {
		BotUserDTO otherUser = botUserManager.getValidBotUserByIdWithParent(senderId, otherUserId);
//		BotUserDTO user = botUserManager.getBotUserByIdWithParent(userId);
		BotCattle cattle = botCattleMapper.getValidBotCattleByUserId(userId);
		BotCattle otherCattle = botCattleMapper.getValidBotCattleByUserId(otherUserId);
		boolean hasItem = botUserItemMappingManager.hasItem(userId, BotItemConstant.CATTLE_ENTANGLEMENT);

		int hasItemFlag = hasItem? 1: -1;
		int sumLength = cattle.getLength() + otherCattle.getLength();
		int rateLimit = MathUtil.range(0, (5 - hasItemFlag * sumLength / 10000), 5);
		int rate = random.nextInt(100);
		int length = random.nextInt(1000);
		log.info(String.format("%s(%s)和%s(%s)比划，hasItem=%s, rateLimit=%s, rate=%s", userId, cattle.getLength(), otherUserId, otherCattle.getLength(), hasItem, rateLimit, rate));

		List<BotMessageChain> respList = new ArrayList<>();
		if (rate < 50 - rateLimit) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, length);
			botCattleRecordMapper.addBotCattleRecordSelective(new BotCattleRecord().setSourceUserId(userId).setTargetUserId(otherUserId).setSourceLengthDiff(-length).setTargetLengthDiff(length).setResult(2).setLength(length));
			if (isRandom) {
				respList.add(BotMessageChain.ofPlain(String.format("与 %s 一番胶战后，输了%.2fcm，还剩%.2fcm。", otherUser.getName(), length / 100.0, (cattle.getLength() - length) / 100.0)));
			} else {
				respList.add(BotMessageChain.ofPlain(String.format("一番胶战后，你输了%.2fcm，还剩%.2fcm。", length / 100.0, (cattle.getLength() - length) / 100.0)));
			}
		} else if (rate < 100 - rateLimit * 2) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, length, -length);
			botCattleRecordMapper.addBotCattleRecordSelective(new BotCattleRecord().setSourceUserId(userId).setTargetUserId(otherUserId).setSourceLengthDiff(length).setTargetLengthDiff(-length).setResult(0).setLength(length));
			if (isRandom) {
				respList.add(BotMessageChain.ofPlain(String.format("与 %s 一番胶战后，赢得了%.2fcm，现在有%.2fcm。", otherUser.getName(), length / 100.0, (cattle.getLength() + length) / 100.0)));
			} else {
				respList.add(BotMessageChain.ofPlain(String.format("一番胶战后，你赢得了%.2fcm，现在有%.2fcm。", length / 100.0, (cattle.getLength() + length) / 100.0)));
			}
		} else {
			if (hasItem){
				botCattleManager.safeCalculateCattle(userId, otherUserId, length, length);
				botCattleRecordMapper.addBotCattleRecordSelective(new BotCattleRecord().setSourceUserId(userId).setTargetUserId(otherUserId).setSourceLengthDiff(length).setTargetLengthDiff(length).setResult(3).setLength(length));
				if (isRandom) {
					respList.add(BotMessageChain.ofPlain(String.format("不好，和 %s 缠在一起了，但在纠缠之缘的作用下，彼此促进，双方都长了%.2fcm。", otherUser.getName(), length / 100.0)));
				} else {
					respList.add(BotMessageChain.ofPlain(String.format("不好，缠在一起了，但在纠缠之缘的作用下，双方都长了%.2fcm。", length / 100.0)));
				}
				Integer subNum = botUserItemMappingManager.addMapping(new BotUserItemMapping().setUserId(userId).setItemId(BotItemConstant.CATTLE_ENTANGLEMENT).setNum(-1));
				Asserts.checkEquals(subNum, -1, "使用失败");
			} else {
				botCattleManager.safeCalculateCattle(userId, otherUserId, -length, -length);
				botCattleRecordMapper.addBotCattleRecordSelective(new BotCattleRecord().setSourceUserId(userId).setTargetUserId(otherUserId).setSourceLengthDiff(-length).setTargetLengthDiff(-length).setResult(1).setLength(length));
				if (isRandom) {
					respList.add(BotMessageChain.ofPlain(String.format("不好，和 %s 缠在一起了，双方都断了%.2fcm。", otherUser.getName(), length / 100.0)));
				} else {
					respList.add(BotMessageChain.ofPlain(String.format("不好，缠在一起了，双方都断了%.2fcm。", length / 100.0)));
				}
			}
		}
		return respList;
	}

	private BotMessage handleRecord(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		Long userId = messageAction.getBotUser().getId();
		BotCattle botCattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(botCattle, "我不倒啊。");
		List<BotCattleRecord> botCattleRecordList = botCattleRecordMapper.getBotCattleRecordByUserId(userId);
		List<String> chainList = new ArrayList<>();
		String title = String.format("当前%.2fcm。", botCattle.getLength() / 100.0);
		chainList.add(title);
		for (int index = 0; index < botCattleRecordList.size(); index++) {
			BotCattleRecord botCattleRecord = botCattleRecordList.get(index);
			Long targetUserId = botCattleRecord.getTargetUserId();
			Integer result = botCattleRecord.getResult();

			boolean isTarget = Objects.equals(targetUserId, userId);
			if (isTarget) {
				targetUserId = botCattleRecord.getSourceUserId();
				if (result < 3) {
					result = 2 - result;
				}
			}
			BotUserDTO targetUser = botUserManager.getValidBotUserByIdWithParent(senderId, targetUserId);

			String targetUserName = targetUser.getName();
			double length = botCattleRecord.getLength() / 100.0;
			String message;
			switch (result) {
				case 0: message = String.format("%s.斩获[%s]%.2fcm", index+1, targetUserName, length);break;
				case 1: message = String.format("%s.和[%s]一起折断了%.2fcm", index+1, targetUserName, length);break;
				case 2: message = String.format("%s.败给[%s]%.2fcm", index+1, targetUserName, length);break;
				case 3: message = String.format("%s.和[%s]一起长了%.2fcm", index+1, targetUserName, length);break;
				default: throw new AssertException();
			}
			if (title.length() + message.length() + chainList.stream().mapToInt(String::length).sum() + chainList.size() > 100) {
				break;
			}
			chainList.add(message);
		}
		return BotMessage.simpleTextMessage(String.join("\n", chainList));
	}

	private BotMessage handleRedemption(BotMessageAction messageAction) {
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		BotCattle botCattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(botCattle, "巧妇难为无米炊。");

		int score = MathUtil.range(0, botUser.getScore(), 1000);
		Asserts.notEquals(score, 0, "欲邀击筑悲歌饮，正值倾家无酒钱。");

		String redemptionLengthStr = messageAction.getValueOrDefault("10");
		Asserts.isNumber(redemptionLengthStr, "格式错啦(长度)");
		int inputLength = MathUtil.range(0, new BigDecimal(redemptionLengthStr).multiply(BigDecimal.valueOf(100)).intValue(), 1000);
		Asserts.notEquals(inputLength, 0, "找茬柿吧");
		int length = MathUtil.range(0, Math.min(-botCattle.getLength(), inputLength), 1000);
		Asserts.notEquals(length, 0, "无病莫呻吟");

		int redemptionLength = Math.min(score, length);

		// 凌晨4点刷新
		String dayStr = DateUtils.formatDateYMD(DateUtils.addTime(new Date(), Calendar.HOUR_OF_DAY, -4));
		Asserts.isTrue(redisCache.putIfAbsent(String.format("redemption-%s-%s", userId, dayStr), "yes", Duration.ofDays(1)), "阿伟，你咋又来赎牛子哦。");

		Integer realRedemptionLength = botUserManager.safeUpdateScore(botUser, -redemptionLength);
		Asserts.checkEquals(realRedemptionLength, -redemptionLength, "啊嘞，不对劲");
		Asserts.checkEquals(botCattleMapper.safeUpdateCattleLength(botCattle.getId(), botCattle.getLength(), redemptionLength), 1, "啊嘞，不对劲");

		return BotMessage.simpleTextMessage(String.format("赎回了%.2fcm，现在有%.2fcm，消耗了%s积分，再接再厉啊。", redemptionLength / 100.0, (botCattle.getLength() + redemptionLength) / 100.0, redemptionLength));
	}

	private BotMessage handleDescRank(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		List<BotCattle> cattleList = botCattleMapper.getBotCattleByCondition(new BotCattleQuery().setStatus(0).setPageSize(20).setSorter("length").setSorted("asc"));
		List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		Set<Long> senderUserIdList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId).collect(Collectors.toSet());
		List<BotCattle> senderCattleList = cattleList.stream().filter(cattle -> senderUserIdList.contains(cattle.getUserId())).limit(5).collect(Collectors.toList());
		String resp = IntStream.range(0, senderCattleList.size()).mapToObj(index -> {
			BotCattle botCattle = senderCattleList.get(index);
			BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(botSender.getId(), botCattle.getUserId());
			return String.format("%s:%.2fcm %s", index + 1, botCattle.getLength() / 100.0, botUser.getName());
		}).collect(Collectors.joining("\n"));
		return BotMessage.simpleTextMessage(resp);
	}

	private BotMessage handleInfo(BotMessageAction messageAction) {
		Long userId = messageAction.getBotUser().getId();
		BotCattle botCattle = botCattleMapper.getValidBotCattleByUserId(userId);
		Asserts.notNull(botCattle, "巧妇难为无米炊。");
		String redisKey = cattleSleepKey + userId;
		Long expire = redisCache.getExpire(redisKey);
		String cdStr = expire > 0? "，圣光微显": "";
		return BotMessage.simpleTextMessage(String.format("足足有%.2fcm长%s", botCattle.getLength() / 100.0, cdStr));
	}

	private BotMessage handleRank(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		List<BotCattle> cattleList = botCattleMapper.getBotCattleByCondition(new BotCattleQuery().setStatus(0).setPageSize(20).setSorter("length").setSorted("desc"));
		List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		Set<Long> senderUserIdList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId).collect(Collectors.toSet());
		List<BotCattle> senderCattleList = cattleList.stream().filter(cattle -> senderUserIdList.contains(cattle.getUserId())).limit(5).collect(Collectors.toList());
		String resp = IntStream.range(0, senderCattleList.size()).mapToObj(index -> {
			BotCattle botCattle = senderCattleList.get(index);
			BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(botSender.getId(), botCattle.getUserId());
			return String.format("%s:%.2fcm %s", index + 1, botCattle.getLength() / 100.0, botUser.getName());
		}).collect(Collectors.joining("\n"));
		return BotMessage.simpleTextMessage(resp);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
//		Asserts.checkEquals(botUser.getType(), BotUserConstant.USER_TYPE_QQ, "未绑定");

		BotCattle botCattle = botCattleMapper.getBotCattleByUserId(userId);
		if (botCattle != null) {
			Asserts.checkEquals(botCattle.getStatus(), -1, "不要太贪心哦");

			int cnt = botCattleMapper.updateBotCattleSelective(new BotCattle().setId(botCattle.getId()).setStatus(0).setStartTime(new Date()));
			Asserts.checkEquals(cnt, 1, "啊嘞，不对劲");
			return BotMessage.simpleTextMessage(String.format("%s带着他的%.2fcm长刀回来了！", botUser.getName(), botCattle.getLength() / 100.0));
		} else {
			int length = random.nextInt(1000);
			botCattleMapper.addBotCattleSelective(new BotCattle().setUserId(userId).setLength(length));

			String tips = botUser.getType() == BotUserConstant.USER_TYPE_QQ ? "" : "(tips：有共同群聊最好先申请合体再领。";
			return BotMessage.simpleTextMessage(String.format("恭喜领到%.2fcm%s", length / 100.0, tips)).setQuote(messageId);
		}
	}
}
