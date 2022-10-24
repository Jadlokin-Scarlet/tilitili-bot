package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MuteHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;

	@Autowired
	public MuteHandle(BotManager botManager) {
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotEmum bot = messageAction.getBot();
		String key = messageAction.getKey();
		List<Long> atList = messageAction.getAtList();
		BotMessage botMessage = messageAction.getBotMessage();
		Asserts.checkEquals(botMessage.getSendType(), SendTypeEmum.GROUP_MESSAGE_STR, "啊嘞，不对劲");
		Long group = botMessage.getGroup();
		Asserts.notEmpty(atList, "格式不对喵(at)");
		Long at = atList.get(0);

		if ("禁言".equals(key)) {
			String timeStr = messageAction.getValueOrDefault("60");
			Asserts.isNumber(timeStr, "格式不对喵(秒数)");
			int time = Integer.parseInt(timeStr);
			Asserts.isTrue(time <= 60, "最多一分钟喵");
			Asserts.notEquals(at, 545459363L, "不准喵");

			botManager.muteMember(bot, group, at, time);
		} else if ("解除禁言".equals(key)) {
			botManager.unMuteMember(bot, group, at);
		}

		return BotMessage.simpleTextMessage("好了喵");
	}
}
