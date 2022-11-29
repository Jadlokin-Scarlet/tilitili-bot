package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberLeaveEventQuit;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MiraiMemberLeaveEventQuitHandle extends MiraiAutoEventHandle<MiraiMemberLeaveEventQuit> {
	private final BotSenderMapper botSenderMapper;
	private final SendMessageManager sendMessageManager;

	@Autowired
	public MiraiMemberLeaveEventQuitHandle(BotSenderMapper botSenderMapper, SendMessageManager sendMessageManager) {
		super(MiraiMemberLeaveEventQuit.class);
		this.botSenderMapper = botSenderMapper;
		this.sendMessageManager = sendMessageManager;
	}

	@Override
	public void handleEvent(BotEmum bot, MiraiMemberLeaveEventQuit event) {
		String message = String.format("%s离开了。", event.getMember().getMemberName());

		BotSender botSender = botSenderMapper.getBotSenderByGroup(event.getMember().getGroup().getId());
		Asserts.checkEquals(bot.id, botSender.getBot(), "没有权限");

		sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
