package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.GocqAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.GocqNoticeNotifyPoke;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class GocqNoticeNotifyPokeHandle extends GocqAutoEventHandle<GocqNoticeNotifyPoke> {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;

	@Autowired
	public GocqNoticeNotifyPokeHandle(BotManager botManager, BotSenderMapper botSenderMapper) {
		super(GocqNoticeNotifyPoke.class);
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
	}

	@Override
	public void handleEvent(GocqNoticeNotifyPoke event) throws Exception {
		log.info(Gsons.toJson(event));
		BotSender botSender;
		if (event.getGroupId() != null) {
			botSender = botSenderMapper.getBotSenderByGroup(event.getGroupId());
		} else {
			botSender = botSenderMapper.getBotSenderByQq(event.getSenderId());
		}
		if (Objects.equals(event.getUserId(), botSender.getBot())) {
			return;
		}
		if (!Objects.equals(event.getTargetId(), botSender.getBot())) {
			return;
		}

		BotEmum bot = BotEmum.getByValue(botSender.getBot());
		Asserts.notNull(bot, "啊咧，不对劲");
		botManager.sendNudge(bot, botSender, event.getUserId());
	}
}
