package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.GocqAutoEventHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.GocqNoticeGroupDecreaseKick;
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
public class GocqNoticeGroupDecreaseKickHandle extends GocqAutoEventHandle<GocqNoticeGroupDecreaseKick> {
	private final BotSenderMapper botSenderMapper;
	private final SendMessageManager sendMessageManager;
	private final BotUserManager botUserManager;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;

	@Autowired
	public GocqNoticeGroupDecreaseKickHandle(BotSenderMapper botSenderMapper, SendMessageManager sendMessageManager, BotUserManager botUserManager, BotUserSenderMappingMapper botUserSenderMappingMapper) {
		super(GocqNoticeGroupDecreaseKick.class);
		this.botSenderMapper = botSenderMapper;
		this.sendMessageManager = sendMessageManager;
		this.botUserManager = botUserManager;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
	}

	@Override
	public void handleEvent(BotEmum bot, GocqNoticeGroupDecreaseKick event) throws Exception {
		BotSender botSender = botSenderMapper.getBotSenderByGroup(event.getGroupId());
		BotUserDTO botUser = botUserManager.getBotUserByExternalIdWithParent(event.getUserId(), BotUserConstant.USER_TYPE_QQ);
		String message = String.format("%s离开了。", botUser == null? event.getUserId(): botUser.getName());

		if (botUser != null) {
			BotUserSenderMapping botUserSenderMapping = botUserSenderMappingMapper.getBotUserSenderMappingBySenderIdAndUserId(botSender.getId(), botUser.getId());
			botUserSenderMappingMapper.deleteBotUserSenderMappingByPrimary(botUserSenderMapping.getId());
		}

		Asserts.checkEquals(bot.id, botSender.getBot(), "没有权限");
		sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
