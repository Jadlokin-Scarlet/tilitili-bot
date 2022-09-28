package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.NudgeEvent;
import com.tilitili.common.entity.view.bot.mirai.event.NudgeSubject;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NudgeEventHandle extends AutoEventHandle<NudgeEvent> {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;

	@Autowired
	public NudgeEventHandle(BotManager botManager, BotSenderMapper botSenderMapper) {
		super(NudgeEvent.class);
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
	}

	@Override
	public void handleEvent(NudgeEvent event) throws Exception {
		NudgeSubject subject = event.getSubject();
		BotSender botSender;
		if ("Group".equals(subject.getKind())) {
			botSender = botSenderMapper.getBotSenderByGroup(subject.getId());
		} else {
			botSender = botSenderMapper.getBotSenderByQq(subject.getId());
		}
		BotEmum bot = BotEmum.getByValue(botSender.getBot());

		botManager.sendNudge(bot, botSender, event.getFromId());
	}
}
