package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class SignHandle extends ExceptionRespMessageHandle {
	private final static String externalIdLockKey = "signHandle.externalIdLockKey";
	private final BotUserMapper botUserMapper;

	@Autowired
	public SignHandle(BotUserMapper botUserMapper) {
		this.botUserMapper = botUserMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();

		switch (key) {
			case "签到": case "qd": return handleSignMessage(messageAction);
			case "积分排行": case "jfph": return handleQueryRankMessage(messageAction);
			case "积分查询": case "jfcx": return handleQueryScoreMessage(messageAction);
			default: return null;
		}
	}

	private BotMessage handleQueryScoreMessage(BotMessageAction messageAction) {
		BotUser botUser = messageAction.getBotUser();
		Asserts.notNull(botUser, "啊嘞，似乎不对劲");
		return BotMessage.simpleTextMessage(String.format("当前积分为%s分。", botUser.getScore()));
	}

	private BotMessage handleQueryRankMessage(BotMessageAction messageAction) {
		List<BotUser> userList = botUserMapper.getBotUserByCondition(new BotUserQuery().setStatus(0).setSorter("score").setSorted("desc").setPageSize(10));
		if (userList.size() > 10) userList = userList.subList(0, 10);

		List<BotMessageChain> result = new ArrayList<>();
		result.add(BotMessageChain.ofPlain("排序:分数\t名称"));
		for (int index = 0; index < userList.size(); index++) {
			BotUser botUser = userList.get(index);
			if (botUser.getScore() <= 100) continue;
			result.add(BotMessageChain.ofPlain(String.format("\n%s:%s\t%s", index + 1, botUser.getScore(), botUser.getName())));
		}
		return BotMessage.simpleListMessage(result);
	}

	public BotMessage handleSignMessage(BotMessageAction messageAction) throws Exception {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		Long externalId = botMessage.getSendType().equals(SendTypeEmum.GUILD_MESSAGE_STR)? botMessage.getTinyId(): botMessage.getQq();
		Asserts.notNull(externalId, "似乎哪里不对劲");
		Date now = new Date();
		if (! session.putIfAbsent(externalIdLockKey + externalId, "lock")) {
			log.info("别签到刷屏");
			return null;
		}

		int addScore;
		try {
			BotUser botUser = botUserMapper.getBotUserByExternalId(externalId);
			Asserts.notNull(botUser, "似乎有什么不对劲");
			if (botUser.getScore() >= 100) {
				log.info("积分满了");
				return null;
			}
			if (botUser.getLastSignTime() != null && botUser.getLastSignTime().after(DateUtils.getCurrentDay())) {
				log.info("已经签到过了");
				return null;
			}
			addScore = Math.max(100 - botUser.getScore(), 0);
			BotUser updBotUser = new BotUser().setId(botUser.getId()).setLastSignTime(now);
			if (addScore != 0) updBotUser.setScore(botUser.getScore() + addScore);
			botUserMapper.updateBotUserSelective(updBotUser);
		} finally {
			session.remove(externalIdLockKey + externalId);
		}


		int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.CHINESE).format(now));
		String time = "早上";
		if (hour > 9) time = "中午";
		if (hour > 12) time = "下午";
		if (hour > 18) time = "晚上";

		String talk = "今天也是充满希望的一天";
		String message1 = String.format("%s好，%s", time, talk);
		String message2 = String.format("(分数+%d)", addScore);
		String message = message1 + (addScore == 0? "": message2);
		return BotMessage.simpleTextMessage(message).setQuote(messageAction.getMessageId());
	}
}
