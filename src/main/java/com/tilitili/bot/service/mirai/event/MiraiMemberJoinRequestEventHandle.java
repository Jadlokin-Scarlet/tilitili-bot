package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotSenderTaskMapping;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberJoinRequestEvent;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class MiraiMemberJoinRequestEventHandle extends MiraiAutoEventHandle<MiraiMemberJoinRequestEvent> {
	public static final String newMemberKey = "MiraiMemberJoinRequestEventHandle.newMember";
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;
	private final BotSessionService botSessionService;
	private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;

	@Autowired
	public MiraiMemberJoinRequestEventHandle(BotManager botManager, BotSenderMapper botSenderMapper, BotSessionService botSessionService, BotSenderTaskMappingMapper botSenderTaskMappingMapper) {
		super(MiraiMemberJoinRequestEvent.class);
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
		this.botSessionService = botSessionService;
		this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
	}

	@Override
	public void handleEvent(MiraiMemberJoinRequestEvent event) {
		String message = String.format("[%s][%s]申请加入[%s][%s]，是否接受(同意加群申请/拒绝加群申请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId());
		BotSender botSender = botSenderMapper.getBotSenderByGroup(event.getGroupId());

		// 校验权限
		List<BotSenderTaskMapping> botSenderTaskMappingList = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderId(botSender.getId());
		boolean hasEventHandle = botSenderTaskMappingList.stream().map(BotSenderTaskMapping::getTaskId).anyMatch(Predicate.isEqual(50L));
		Asserts.isTrue(hasEventHandle, "啊嘞，不对劲。");

		BotSessionService.MiraiSession session = botSessionService.getSession(botSessionService.getSessionKey(botSender));
		session.put(newMemberKey, Gsons.toJson(event));
		session.put(newMemberKey + "-" + event.getFromId(), Gsons.toJson(event));
		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
