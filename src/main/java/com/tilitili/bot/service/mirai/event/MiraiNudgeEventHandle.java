package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNudgeEvent;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNudgeSubject;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MiraiNudgeEventHandle extends MiraiAutoEventHandle<MiraiNudgeEvent> {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;

	@Autowired
	public MiraiNudgeEventHandle(BotManager botManager, BotSenderMapper botSenderMapper) {
		super(MiraiNudgeEvent.class);
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
	}

	@Override
	public void handleEvent(MiraiNudgeEvent event) throws Exception {
		MiraiNudgeSubject subject = event.getSubject();

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
