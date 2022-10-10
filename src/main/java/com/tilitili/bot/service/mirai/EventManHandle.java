package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.event.MiraiBotInvitedJoinGroupRequestEventHandle;
import com.tilitili.bot.service.mirai.event.MiraiMemberJoinRequestEventHandle;
import com.tilitili.bot.service.mirai.event.MiraiNewFriendRequestEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiBotInvitedJoinGroupRequestEvent;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiMemberJoinRequestEvent;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNewFriendRequestEvent;
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
			case "同意群邀请": return handleMiraiGroupInviteEvent(messageAction);
			case "同意好友邀请": return handleMiraiNewFriendEvent(messageAction);
			case "同意加群申请": return handleMiraiMemberJoinRequestEvent(messageAction);
		}
		return null;
	}

	private BotMessage handleMiraiNewFriendEvent(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		if (redisCache.exists(MiraiNewFriendRequestEventHandle.newFriendKey)) {
			MiraiNewFriendRequestEvent event = (MiraiNewFriendRequestEvent) redisCache.getValue(MiraiNewFriendRequestEventHandle.newFriendKey);
			miraiManager.handleMiraiNewFriendRequestEvent(bot, event);
			redisCache.delete(MiraiNewFriendRequestEventHandle.newFriendKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleMiraiGroupInviteEvent(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		if (redisCache.exists(MiraiBotInvitedJoinGroupRequestEventHandle.newGroupKey)) {
			MiraiBotInvitedJoinGroupRequestEvent event = (MiraiBotInvitedJoinGroupRequestEvent) redisCache.getValue(MiraiBotInvitedJoinGroupRequestEventHandle.newGroupKey);
			miraiManager.handleMiraiBotInvitedJoinGroupRequestEvent(bot, event);
			redisCache.delete(MiraiBotInvitedJoinGroupRequestEventHandle.newGroupKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleMiraiMemberJoinRequestEvent(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotEmum bot = messageAction.getBot();
		String value = messageAction.getValue();
		String key = StringUtils.isBlank(value) ? MiraiMemberJoinRequestEventHandle.newMemberKey : MiraiMemberJoinRequestEventHandle.newMemberKey + "-" + value;
		if (session.containsKey(key)) {
			MiraiMemberJoinRequestEvent event = Gsons.fromJson(session.get(key), MiraiMemberJoinRequestEvent.class);
			miraiManager.handleMiraiMemberJoinRequestEvent(bot, event);
			session.remove(key);
			session.remove(MiraiMemberJoinRequestEventHandle.newMemberKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}
}
