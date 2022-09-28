package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.NudgeEvent;
import com.tilitili.common.entity.view.bot.mirai.event.NudgeSubject;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
		if (Objects.equals(event.getFromId(), botSender.getBot())) {
			return;
		}
		if (!Objects.equals(event.getTarget(), botSender.getBot())) {
			return;
		}

		BotEmum bot = BotEmum.getByValue(botSender.getBot());
		Asserts.notNull(bot, "啊咧，不对劲");
		botManager.sendNudge(bot, botSender, event.getFromId());
	}
}
