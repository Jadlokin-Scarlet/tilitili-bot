package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberJoinRequestEventHandle extends BaseEventHandleAdapt {
	public static final String newMemberKey = "MemberJoinRequestEventHandle.newMember";
	private final BotSessionService botSessionService;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public MemberJoinRequestEventHandle(BotSessionService botSessionService, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		super(BotEvent.EVENT_TYPE_MEMBER_JOIN_REQUEST);
		this.botSessionService = botSessionService;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public BotMessage handleEvent(BotRobot bot, BotMessage botMessage) {
		BotSender botSender = botMessage.getBotSender();
		BotUserDTO botUser = botMessage.getBotUser();
		BotEvent botEvent = botMessage.getBotEvent();

		String message = String.format("[%s][%s]申请加入[%s][%s]，备注[%s]，是否接受(同意加群申请/拒绝加群申请)", botUser.getName(), botUser.getQq(), botSender.getName(), botSender.getGroup(), botEvent.getMessage());

		// 校验权限
		boolean hasEventHandle = botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.EventManTaskId);
		Asserts.isTrue(hasEventHandle, "啊嘞，不对劲。");

		BotSessionService.MiraiSession session = botSessionService.getSession(botSender.getId());
		session.put(newMemberKey, Gsons.toJson(botMessage));
		session.put(newMemberKey + "-" + botSender.getId(), Gsons.toJson(botMessage));
		return BotMessage.simpleTextMessage(message);
	}
}
