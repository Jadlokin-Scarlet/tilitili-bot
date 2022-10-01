package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.event.BotInvitedJoinGroupRequestEventHandle;
import com.tilitili.bot.service.mirai.event.MemberJoinRequestEventHandle;
import com.tilitili.bot.service.mirai.event.NewFriendRequestEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.BotInvitedJoinGroupRequestEvent;
import com.tilitili.common.entity.view.bot.mirai.event.MemberJoinRequestEvent;
import com.tilitili.common.entity.view.bot.mirai.event.NewFriendRequestEvent;
import com.tilitili.common.manager.MiraiManager;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventManHandle extends ExceptionRespMessageHandle {
	private final RedisCache redisCache;
	private final MiraiManager miraiManager;

	@Autowired
	public EventManHandle(RedisCache redisCache, MiraiManager miraiManager) {
		this.redisCache = redisCache;
		this.miraiManager = miraiManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKey();
		switch (key) {
			case "同意群邀请": return handleGroupInviteEvent(messageAction);
			case "同意好友邀请": return handleNewFriendEvent(messageAction);
			case "同意加群申请": return handleMemberJoinRequestEvent(messageAction);
		}
		return null;
	}

	private BotMessage handleNewFriendEvent(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		if (redisCache.exists(NewFriendRequestEventHandle.newFriendKey)) {
			NewFriendRequestEvent event = (NewFriendRequestEvent) redisCache.getValue(NewFriendRequestEventHandle.newFriendKey);
			miraiManager.handleNewFriendRequestEvent(bot, event);
			redisCache.delete(NewFriendRequestEventHandle.newFriendKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleGroupInviteEvent(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		if (redisCache.exists(BotInvitedJoinGroupRequestEventHandle.newGroupKey)) {
			BotInvitedJoinGroupRequestEvent event = (BotInvitedJoinGroupRequestEvent) redisCache.getValue(BotInvitedJoinGroupRequestEventHandle.newGroupKey);
			miraiManager.handleBotInvitedJoinGroupRequestEvent(bot, event);
			redisCache.delete(BotInvitedJoinGroupRequestEventHandle.newGroupKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleMemberJoinRequestEvent(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotEmum bot = messageAction.getBot();
		String value = messageAction.getValue();
		String key = StringUtils.isBlank(value) ? MemberJoinRequestEventHandle.newMemberKey : MemberJoinRequestEventHandle.newMemberKey + "-" + value;
		if (session.containsKey(key)) {
			MemberJoinRequestEvent event = Gsons.fromJson(session.get(key), MemberJoinRequestEvent.class);
			miraiManager.handleMemberJoinRequestEvent(bot, event);
			session.remove(key);
			session.remove(MemberJoinRequestEventHandle.newMemberKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}
}
