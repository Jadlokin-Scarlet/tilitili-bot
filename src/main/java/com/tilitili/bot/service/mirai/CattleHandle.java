package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotCattle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotCattleQuery;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotCattleManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotCattleMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CattleHandle extends ExceptionRespMessageToSenderHandle {
	private final BotCattleMapper botCattleMapper;
	private final BotCattleManager botCattleManager;
	private final RedisCache redisCache;
	private final BotUserManager botUserManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	private final Random random;

	@Autowired
	public CattleHandle(BotCattleMapper botCattleMapper, BotCattleManager botCattleManager, RedisCache redisCache, BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper) {
		this.botCattleMapper = botCattleMapper;
		this.botCattleManager = botCattleManager;
		this.redisCache = redisCache;
		this.botUserManager = botUserManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "我的牛子": return handleInfo(messageAction);
			case "领取牛子": return handleStart(messageAction);
			case "比划比划": return handlePk(messageAction);
			case "牛子榜": return handleRank(messageAction);
			case "牛子0榜": return handleDescRank(messageAction);
		}
		return null;
	}

	private BotMessage handleDescRank(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		List<BotCattle> cattleList = botCattleMapper.getBotCattleByCondition(new BotCattleQuery().setPageSize(20).setSorter("length").setSorted("asc"));
		List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		Set<Long> senderUserIdList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId).collect(Collectors.toSet());
		List<BotCattle> senderCattleList = cattleList.stream().filter(cattle -> senderUserIdList.contains(cattle.getUserId())).limit(5).collect(Collectors.toList());
		String resp = IntStream.range(0, senderCattleList.size()).mapToObj(index -> String.format("%s:%.2fcm %s", index+1, senderCattleList.get(index).getLength() / 100.0, botUserManager.getBotUserByIdWithParent(senderCattleList.get(index).getUserId()).getName())).collect(Collectors.joining("\n"));
		return BotMessage.simpleTextMessage(resp);
	}

	private BotMessage handleInfo(BotMessageAction messageAction) {
		BotCattle botCattle = botCattleMapper.getBotCattleByUserId(messageAction.getBotUser().getId());
		Asserts.notNull(botCattle, "巧妇难为无米炊。");
		return BotMessage.simpleTextMessage(String.format("足足有%.2fcm长", botCattle.getLength() / 100.0));
	}

	private BotMessage handleRank(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		List<BotCattle> cattleList = botCattleMapper.getBotCattleByCondition(new BotCattleQuery().setPageSize(20).setSorter("length").setSorted("desc"));
		List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
		Set<Long> senderUserIdList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId).collect(Collectors.toSet());
		List<BotCattle> senderCattleList = cattleList.stream().filter(cattle -> senderUserIdList.contains(cattle.getUserId())).limit(5).collect(Collectors.toList());
		String resp = IntStream.range(0, senderCattleList.size()).mapToObj(index -> String.format("%s:%.2fcm %s", index+1, senderCattleList.get(index).getLength() / 100.0, botUserManager.getBotUserByIdWithParent(senderCattleList.get(index).getUserId()).getName())).collect(Collectors.joining("\n"));
		return BotMessage.simpleTextMessage(resp);
	}

	private BotMessage handlePk(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		String redisKey = String.format("CattleHandle-%s", userId);
		Long expire = redisCache.getExpire(redisKey);
		Asserts.isTrue(expire <= 0, "节制啊，再休息%s吧", expire > 60? expire/60+"分钟": expire+"秒");

		BotCattle cattle = botCattleMapper.getBotCattleByUserId(userId);
		Asserts.notNull(cattle, "巧妇难为无米炊。");

		BotCattle otherCattle;
		boolean isRandom;
		List<Long> atList = messageAction.getAtList();
		if (atList.isEmpty()) {
			List<BotUserSenderMapping> botUserSenderMappingList = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setSenderId(botSender.getId()));
			List<BotCattle> senderCattleList = botUserSenderMappingList.stream().map(BotUserSenderMapping::getUserId)
					.filter(Predicate.isEqual(userId).negate())
					.filter(otherUserId -> redisCache.getExpire(String.format("CattleHandle-%s", otherUserId)) <= 0)
					.map(botCattleMapper::getBotCattleByUserId)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (!senderCattleList.isEmpty()) {
				otherCattle = senderCattleList.get(random.nextInt(senderCattleList.size()));
			} else {
				otherCattle = null;
			}
			isRandom = true;
		} else {
			Long otherUserId = atList.get(0);
			Asserts.notEquals(userId, otherUserId, "你找茬是⑧");
			otherCattle = botCattleMapper.getBotCattleByUserId(otherUserId);
			isRandom = false;
		}
		Asserts.notNull(otherCattle, "拔剑四顾心茫然。");

		Long otherUserId = otherCattle.getUserId();
		String otherRedisKey = String.format("CattleHandle-%s", otherUserId);
		Long otherExpire = redisCache.getExpire(otherRedisKey);
		Asserts.isTrue(otherExpire <= 0, "让他再休息%s吧", otherExpire > 60? otherExpire/60+"分钟": otherExpire+"秒");

		int rate = random.nextInt(100);
		int length = random.nextInt(1000);
		List<BotMessageChain> respList = new ArrayList<>();
		if (rate < 45) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, length, -length);
			if (isRandom) {
				respList.add(BotMessageChain.ofPlain("你与"));
				respList.add(BotMessageChain.ofAt(otherUserId));
			}
			respList.add(BotMessageChain.ofPlain(String.format("一番胶战后，你赢得了%.2fcm。", length / 100.0)));
		} else if (rate < 90) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, length);
			if (isRandom) {
				respList.add(BotMessageChain.ofPlain("你与"));
				respList.add(BotMessageChain.ofAt(otherUserId));
			}
			respList.add(BotMessageChain.ofPlain(String.format("一番胶战后，你输了%.2fcm。", length / 100.0)));
		} else {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, -length);
			respList.add(BotMessageChain.ofPlain("不好，"));
			if (isRandom) {
				respList.add(BotMessageChain.ofPlain("和"));
				respList.add(BotMessageChain.ofAt(otherUserId));
			}
			respList.add(BotMessageChain.ofPlain(String.format("缠在一起了，双方都断了%.2fcm。", length / 100.0)));
		}

		redisCache.setValue(redisKey, "yes", 60*60);
		redisCache.setValue(otherRedisKey, "yes", 60*60);
		return BotMessage.simpleListMessage(respList).setQuote(messageId);
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		Asserts.checkNull(botCattleMapper.getBotCattleByUserId(userId), "不要太贪心哦");
		int length = random.nextInt(1000);
		botCattleMapper.addBotCattleSelective(new BotCattle().setUserId(userId).setLength(length));
		return BotMessage.simpleTextMessage(String.format("恭喜领到%.2fcm", length / 100.0)).setQuote(messageId);
	}
}
