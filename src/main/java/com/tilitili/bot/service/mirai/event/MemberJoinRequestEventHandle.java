package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MemberJoinRequestEvent;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberJoinRequestEventHandle extends AutoEventHandle<MemberJoinRequestEvent> {
	public static final String newMemberKey = "MemberJoinRequestEvent.newMember";
	private final RedisCache redisCache;
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;
	private final BotSessionService botSessionService;

	@Autowired
	public MemberJoinRequestEventHandle(BotManager botManager, RedisCache redisCache, BotSenderMapper botSenderMapper, BotSessionService botSessionService) {
		super(MemberJoinRequestEvent.class);
		this.botManager = botManager;
		this.redisCache = redisCache;
		this.botSenderMapper = botSenderMapper;
		this.botSessionService = botSessionService;
	}

	@Override
	public void handleEvent(MemberJoinRequestEvent event) {
		String message = String.format("[%s][%s]申请加入[%s][%s]，是否接受(同意加群申请/拒绝加群申请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId());
		BotSender botSender = botSenderMapper.getBotSenderByGroup(event.getGroupId());
		BotSessionService.MiraiSession session = botSessionService.getSession(botSessionService.getSessionKey(botSender));
		session.put(newMemberKey, Gsons.toJson(event));
		session.put(newMemberKey + "-" + event.getFromId(), Gsons.toJson(event));
		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(botSender.getId()));
	}
}
