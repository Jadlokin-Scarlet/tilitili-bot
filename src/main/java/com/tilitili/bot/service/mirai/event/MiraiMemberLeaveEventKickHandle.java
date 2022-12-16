package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberLeaveEventKick;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MiraiMemberLeaveEventKickHandle extends MiraiAutoEventHandle<MiraiMemberLeaveEventKick> {
	private final BotSenderMapper botSenderMapper;
	private final SendMessageManager sendMessageManager;
	private final BotUserManager botUserManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public MiraiMemberLeaveEventKickHandle(BotSenderMapper botSenderMapper, SendMessageManager sendMessageManager, BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		super(MiraiMemberLeaveEventKick.class);
		this.botSenderMapper = botSenderMapper;
		this.sendMessageManager = sendMessageManager;
		this.botUserManager = botUserManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public void handleEvent(BotEmum bot, MiraiMemberLeaveEventKick event) {
		BotSender botSender = botSenderMapper.getValidBotSenderByGroup(event.getMember().getGroup().getId());
		BotUserDTO botUser = botUserManager.getBotUserByExternalIdWithParent(event.getMember().getId(), BotUserConstant.USER_TYPE_QQ);
		String message = String.format("%s离开了。", event.getMember().getMemberName());

		if (botUser != null) {
			BotUserSenderMapping botUserSenderMapping = botUserSenderMappingMapper.getBotUserSenderMappingBySenderIdAndUserId(botSender.getId(), botUser.getId());
			if (botUserSenderMapping != null) {
				botUserSenderMappingMapper.deleteBotUserSenderMappingByPrimary(botUserSenderMapping.getId());
			}
		}

		Asserts.notNull(botSender, "无权限");
		Asserts.checkEquals(bot.id, botSender.getBot(), "没有权限");
		boolean canHandle = botSenderTaskMappingManager.checkSenderHasTaskCache(botSender.getId(), BotTaskConstant.EventManTaskId);
		Asserts.isTrue(canHandle, "无权限");
		sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
