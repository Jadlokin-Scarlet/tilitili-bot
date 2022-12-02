package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotCattle;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotCattleQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotCattleManager;
import com.tilitili.common.mapper.mysql.BotCattleMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class CattleHandle extends ExceptionRespMessageToSenderHandle {
	private final BotCattleMapper botCattleMapper;
	private final BotCattleManager botCattleManager;

	private final Random random;

	@Autowired
	public CattleHandle(BotCattleMapper botCattleMapper, BotCattleManager botCattleManager) {
		this.botCattleMapper = botCattleMapper;
		this.botCattleManager = botCattleManager;
		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "领取牛子": return handleStart(messageAction);
			case "比划比划": return handlePk(messageAction);
			case "牛子榜": return handleRank(messageAction);
		}
		return null;
	}

	private BotMessage handleRank(BotMessageAction messageAction) {
		List<BotCattle> cattleList = botCattleMapper.getBotCattleByCondition(new BotCattleQuery().setPageSize(5).setSorter("length").setSorted("desc"));
//		cattleList.stream().map()
		return null;
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

		int rate = random.nextInt(100);
		int length = random.nextInt(1000);
		// 发起者获胜
		if (rate < 45) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, length, -length);
			return BotMessage.simpleTextMessage(String.format("恭喜你赢得了%02fcm。", length / 100.0)).setQuote(messageId);
		} else if (rate < 90) {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, length);
			return BotMessage.simpleTextMessage(String.format("可惜你输了%02fcm。", length / 100.0)).setQuote(messageId);
		} else {
			botCattleManager.safeCalculateCattle(userId, otherUserId, -length, -length);
			return BotMessage.simpleTextMessage(String.format("不好，缠在一起了，双方都断了%02fcm。", length / 100.0)).setQuote(messageId);
		}
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		String messageId = messageAction.getMessageId();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		Asserts.notNull(botCattleMapper.getBotCattleByUserId(userId), "不要太贪心哦");
		int length = random.nextInt(1000);
		botCattleMapper.addBotCattleSelective(new BotCattle().setUserId(userId).setLength(length));
		return BotMessage.simpleTextMessage(String.format("恭喜领到%02fcm", length / 100.0)).setQuote(messageId);
	}
}
