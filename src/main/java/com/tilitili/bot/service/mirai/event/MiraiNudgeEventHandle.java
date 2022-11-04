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
	public void handleEvent(BotEmum bot, MiraiNudgeEvent event) throws Exception {
		MiraiNudgeSubject subject = event.getSubject();

		BotSender botSender;
		if ("Group".equals(subject.getKind())) {
			botSender = botSenderMapper.getBotSenderByGroup(subject.getId());
		} else {
			botSender = botSenderMapper.getBotSenderByQq(subject.getId());
		}
		Asserts.checkEquals(botSender.getBot(), bot.id, "bot不匹配，跳过");
		Asserts.notEquals(event.getFromId(), bot.qq, "发起者为本人，跳过");
		Asserts.checkEquals(event.getTarget(), bot.qq, "目标不为bot，跳过");

		botManager.sendNudge(bot, botSender, event.getFromId());
	}
}
