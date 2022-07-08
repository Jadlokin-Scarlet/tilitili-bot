package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
@Component
public class SignHandle extends ExceptionRespMessageHandle {
	private final BotUserMapper botUserMapper;
	private final static String externalIdLockKey = "signHandle.externalIdLockKey";

	@Autowired
	public SignHandle(BotUserMapper botUserMapper) {
		this.botUserMapper = botUserMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		Long externalId = botMessage.getSendType().equals(SendTypeEmum.GUILD_MESSAGE_STR)? botMessage.getTinyId(): botMessage.getQq();
		Asserts.notNull(externalId, "似乎哪里不对劲");
		if (! session.putIfAbsent(externalIdLockKey + externalId, "lock")) {
			log.info("别签到刷屏");
			return null;
		}

		try {
			Date now = new Date();
			BotUser botUser = botUserMapper.getBotUserByExternalId(externalId);
			Asserts.notNull(botUser, "似乎有什么不对劲");
			if (botUser.getLastSignTime() != null && botUser.getLastSignTime().after(DateUtils.getCurrentDay())) {
				log.info("已经签到过了");
				return null;
			}
			botUserMapper.updateBotUserSelective(new BotUser().setId(botUser.getId()).setScore(botUser.getScore() + 100).setLastSignTime(now));

			int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.CHINESE).format(now));
			String time = "早上";
			if (hour > 9) time = "中午";
			if (hour > 12) time = "下午";
			if (hour > 18) time = "晚上";

			String talk = "今天也是充满希望的一天";
			return BotMessage.simpleTextMessage(String.format("%s好，%s(分数+100)", time, talk)).setQuote(messageAction.getMessageId());
		} finally {
			session.remove(externalIdLockKey + externalId);
		}
	}
}
