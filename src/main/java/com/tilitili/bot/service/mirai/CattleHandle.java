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
import com.tilitili.common.manager.BotCattleManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotCattleMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;
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
		}
		return null;
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
		String resp = IntStream.range(0, senderCattleList.size()).mapToObj(index -> String.format("%s:%.2fcm\t%s", index, senderCattleList.get(index).getLength() / 100.0, botUserManager.getBotUserByIdWithParent(senderCattleList.get(index).getUserId()).getName())).collect(Collectors.joining("\n"));
		return BotMessage.simpleTextMessage(resp);
	}

	private BotMessage handlePk(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		List<Long> atList = messageAction.getAtList();
		Asserts.notEmpty(atList, "和谁比划？");
		Long otherUserId = atList.get(0);

		BotCattle cattle = botCattleMapper.getBotCattleByUserId(userId);
		BotCattle otherCattle = botCattleMapper.getBotCattleByUserId(otherUserId);
		Asserts.notNull(cattle, "巧妇难为无米炊。");
		Asserts.notNull(otherCattle, "拔剑四顾心茫然。");

		String redisKey = String.format("CattleHandle-%s", userId);
		Long expire = redisCache.getExpire(redisKey);
		Asserts.isTrue(expire <= 0, "节制啊，再休息%s吧", expire > 60? expire/60+"分钟": expire+"秒");

		String otherRedisKey = String.format("CattleHandle-%s", otherUserId);
		Long otherExpire = redisCache.getExpire(otherRedisKey);
		Asserts.isTrue(otherExpire <= 0, "让他再休息%s吧", otherExpire > 60? otherExpire/60+"分钟": otherExpire+"秒");

		int rate = random.nextInt(100);
		int length = random.nextInt(1000);
		BotMessage resp;
		if (rate < 45) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, length, -length);
			resp = BotMessage.simpleTextMessage(String.format("一番胶战后，你赢得了%.2fcm。", length / 100.0)).setQuote(messageId);
		} else if (rate < 90) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, length);
			resp = BotMessage.simpleTextMessage(String.format("一番胶战后，你输了%.2fcm。", length / 100.0)).setQuote(messageId);
		} else {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, -length);
			resp = BotMessage.simpleTextMessage(String.format("不好，缠在一起了，双方都断了%.2fcm。", length / 100.0)).setQuote(messageId);
		}

		redisCache.setValue(redisKey, "yes", 60*60);
		redisCache.setValue(otherRedisKey, "yes", 60*60);
		return resp;
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
