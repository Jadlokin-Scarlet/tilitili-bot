package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberJoinRequestEvent;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiraiMemberJoinRequestEventHandle extends MiraiAutoEventHandle<MiraiMemberJoinRequestEvent> {
	public static final String newMemberKey = "MiraiMemberJoinRequestEventHandle.newMember";
	private final BotSenderMapper botSenderMapper;
	private final BotSessionService botSessionService;
	private final SendMessageManager sendMessageManager;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public MiraiMemberJoinRequestEventHandle(BotSenderMapper botSenderMapper, BotSessionService botSessionService, SendMessageManager sendMessageManager, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		super(MiraiMemberJoinRequestEvent.class);
		this.botSenderMapper = botSenderMapper;
		this.botSessionService = botSessionService;
		this.sendMessageManager = sendMessageManager;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public void handleEvent(BotEnum bot, MiraiMemberJoinRequestEvent event) {
		String message = String.format("[%s][%s]申请加入[%s][%s]，备注[%s]，是否接受(同意加群申请/拒绝加群申请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId(), event.getMessage());
		BotSender botSender = botSenderMapper.getValidBotSenderByGroup(event.getGroupId());
		Asserts.notNull(botSender, "无权限");
		Asserts.checkEquals(bot.id, botSender.getBot(), "没有权限");

		// 校验权限
		boolean hasEventHandle = botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.EventManTaskId);
		Asserts.isTrue(hasEventHandle, "啊嘞，不对劲。");

		BotSessionService.MiraiSession session = botSessionService.getSession(botSender.getId());
		session.put(newMemberKey, Gsons.toJson(event));
		session.put(newMemberKey + "-" + event.getFromId(), Gsons.toJson(event));
		sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
