package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NudgeEventHandle extends BaseEventHandleAdapt {
	private final BotManager botManager;

	@Autowired
	public NudgeEventHandle(BotManager botManager) {
		super(BotEvent.EVENT_TYPE_NUDGE);
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleEvent(BotRobot bot, BotMessage botMessage) {
		BotSender botSender = botMessage.getBotSender();
		BotUserDTO botUser = botMessage.getBotUser();
		BotEvent botEvent = botMessage.getBotEvent();

		if (BotUserConstant.BOT_USER_ID_LIST.contains(botUser.getId())) {
			log.info("发起者为bot，跳过");
			return null;
		}
		if (!BotUserConstant.BOT_USER_ID_LIST.contains(botEvent.getTarget())) {
			log.info("目标不为bot，跳过");
			return null;
		}

		botManager.sendNudge(bot, botSender, botUser);
		return BotMessage.emptyMessage();
	}
}
