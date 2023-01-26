package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemberLeaveEventHandle extends BaseEventHandleAdapt {
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public MemberLeaveEventHandle(BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		super(BotEvent.EVENT_TYPE_MEMBER_LEAVE);
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public BotMessage handleEvent(BotEnum bot, BotMessage botMessage) {
		BotSender botSender = botMessage.getBotSender();
		BotUserDTO botUser = botMessage.getBotUser();

		String message = String.format("%s离开了。", botUser.getName());

		BotUserSenderMapping botUserSenderMapping = botUserSenderMappingMapper.getBotUserSenderMappingBySenderIdAndUserId(botSender.getId(), botUser.getId());
		if (botUserSenderMapping != null) {
			botUserSenderMappingMapper.deleteBotUserSenderMappingByPrimary(botUserSenderMapping.getId());
		}

		boolean canHandle = botSenderTaskMappingManager.checkSenderHasTaskCache(botSender.getId(), BotTaskConstant.EventManTaskId);
		Asserts.isTrue(canHandle, "无权限");
		return BotMessage.simpleTextMessage(message);
	}
}
