package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberLeaveEventQuit;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiraiMemberLeaveEventQuitHandle extends MiraiAutoEventHandle<MiraiMemberLeaveEventQuit> {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;

	@Autowired
	public MiraiMemberLeaveEventQuitHandle(BotManager botManager, BotSenderMapper botSenderMapper) {
		super(MiraiMemberLeaveEventQuit.class);
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
	}

	@Override
	public void handleEvent(BotEmum bot, MiraiMemberLeaveEventQuit event) {
		String message = String.format("%s离开了。", event.getMember().getMemberName());

		BotSender botSender = botSenderMapper.getBotSenderByGroup(event.getMember().getGroup().getId());
		Asserts.checkEquals(bot.id, botSender.getQq(), "没有权限");

		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
