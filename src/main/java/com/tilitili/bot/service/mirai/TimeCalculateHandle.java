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
			case "疯狂星期四": return handleKfcMessage(messageAction);
			default: return null;
		}
	}

	private BotMessage handleKfcMessage(BotMessageAction messageAction) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, 4);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		long time = calendar.getTimeInMillis() - System.currentTimeMillis();
		if (time < - 1000 * 60 * 60 * 24) return BotMessage.simpleTextMessage("这周的已经过啦！");
		else if (time < 0) return BotMessage.simpleTextMessage("就是今天！");
		long second = time / 1000 % 60;
		long minute = time / 1000 / 60 % 60;
		long hour = time / 1000 / 60 / 60 % 24;
		long day = time / 1000 / 60 / 60 / 24;

		if (day > 0) {
			return BotMessage.simpleTextMessage(String.format("还有%d天%d小时%d分%d秒哦", day, hour, minute, second));
		} else if (hour > 0) {
			return BotMessage.simpleTextMessage(String.format("还有%d小时%d分%d秒哦", hour, minute, second));
		} else if (minute > 0) {
			return BotMessage.simpleTextMessage(String.format("还有%d分%d秒哦", minute, second));
		} else {
			return BotMessage.simpleTextMessage(String.format("还有%d秒哦", second));
		}
	}

	private BotMessage handleGoOffWorkMessage(BotMessageAction messageAction) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 18);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		long time = calendar.getTimeInMillis() - System.currentTimeMillis();
		if (time < 0) return BotMessage.simpleTextMessage("已经到点啦！");
		long second = time / 1000 % 60;
		long minute = time / 1000 / 60 % 60;
		long hour = time / 1000 / 60 / 60;

		if (hour > 0) {
			return BotMessage.simpleTextMessage(String.format("还有%d小时%d分%d秒哦", hour, minute, second));
		} else if (minute > 0) {
			return BotMessage.simpleTextMessage(String.format("还有%d分%d秒哦", minute, second));
		} else {
			return BotMessage.simpleTextMessage(String.format("还有%d秒哦", second));
		}
	}
}
