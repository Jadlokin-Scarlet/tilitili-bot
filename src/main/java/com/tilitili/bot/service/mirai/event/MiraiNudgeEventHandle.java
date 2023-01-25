package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNudgeEvent;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNudgeSubject;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
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
	public void handleEvent(BotEnum bot, MiraiNudgeEvent event) throws Exception {
		MiraiNudgeSubject subject = event.getSubject();

		BotSender botSender;
		if ("Group".equals(subject.getKind())) {
			botSender = botSenderMapper.getValidBotSenderByGroup(subject.getId());
		} else {
			botSender = botSenderMapper.getValidBotSenderByQq(subject.getId());
		}
		Asserts.notNull(botSender, "无权限");
		if (!Objects.equals(botSender.getBot(), bot.id)) {
			log.info("bot不匹配，跳过");
			return;
		}
		if (Objects.equals(event.getFromId(), bot.qq)) {
			log.info("发起者为本人，跳过");
			return;
		}
		if (!Objects.equals(event.getTarget(), bot.qq)) {
			log.info("目标不为bot，跳过");
			return;
		}

		botManager.sendNudge(bot, botSender, event.getFromId());
	}
}
