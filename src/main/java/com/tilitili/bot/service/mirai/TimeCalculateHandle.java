package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.stereotype.Component;

import java.util.Calendar;

@Component
public class TimeCalculateHandle extends ExceptionRespMessageHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String value = messageAction.getValue();
		if (value == null) return null;

		switch (value) {
			case "下班": return handleGoOffWorkMessage(messageAction);
			default: return null;
		}
	}

	private BotMessage handleGoOffWorkMessage(BotMessageAction messageAction) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 18);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		long time = calendar.getTimeInMillis() - System.currentTimeMillis();
		long second = time / 1000 % 60;
		long minute = time / 1000 / 60 % 60;
		long hour = time / 1000 / 60 / 60;

		return BotMessage.simpleTextMessage(String.format("还有%d小时%d分%d秒哦", hour, minute, second));
	}
}
