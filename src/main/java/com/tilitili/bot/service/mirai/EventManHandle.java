package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.bot.service.mirai.event.FriendRequestEventHandle;
import com.tilitili.bot.service.mirai.event.InvitedJoinGroupEventHandle;
import com.tilitili.bot.service.mirai.event.MemberJoinRequestEventHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventManHandle extends ExceptionRespMessageHandle {
	private final RedisCache redisCache;
	private final BotManager botManager;

	@Autowired
	public EventManHandle(RedisCache redisCache, BotManager botManager) {
		this.redisCache = redisCache;
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "同意群邀请": return handleMiraiGroupInviteEvent(messageAction);
			case "同意好友邀请": return handleMiraiNewFriendEvent(messageAction);
			case "同意加群申请": return handleMiraiMemberJoinRequestEvent(messageAction);
		}
		return null;
	}

	private BotMessage handleMiraiNewFriendEvent(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		if (redisCache.exists(FriendRequestEventHandle.newFriendKey)) {
			BotMessage botMessage = (BotMessage) redisCache.getValue(FriendRequestEventHandle.newFriendKey);
			botManager.handleFriendRequestEvent(bot, botMessage);
			redisCache.delete(FriendRequestEventHandle.newFriendKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleMiraiGroupInviteEvent(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		if (redisCache.exists(InvitedJoinGroupEventHandle.newGroupKey)) {
			BotMessage botMessage = (BotMessage) redisCache.getValue(InvitedJoinGroupEventHandle.newGroupKey);
			botManager.handleInvitedJoinGroupEvent(bot, botMessage);
			redisCache.delete(InvitedJoinGroupEventHandle.newGroupKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}

	private BotMessage handleMiraiMemberJoinRequestEvent(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotRobot bot = messageAction.getBot();
		String value = messageAction.getValue();
		String key = StringUtils.isBlank(value) ? MemberJoinRequestEventHandle.newMemberKey : MemberJoinRequestEventHandle.newMemberKey + "-" + value;
		if (session.containsKey(key)) {
			BotMessage botMessage = Gsons.fromJson(session.get(key), BotMessage.class);
			botManager.handleMemberJoinRequestEvent(bot, botMessage);
			session.remove(key);
			session.remove(MemberJoinRequestEventHandle.newMemberKey);
			return BotMessage.simpleTextMessage("好的");
		}
		return null;
	}
}
