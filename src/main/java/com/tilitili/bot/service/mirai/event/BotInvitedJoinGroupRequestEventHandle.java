package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.entity.mirai.event.BotInvitedJoinGroupRequestEvent;
import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BotInvitedJoinGroupRequestEventHandle extends AutoEventHandle<BotInvitedJoinGroupRequestEvent> {
	public static final String newGroupKey = "BotInvitedJoinGroupRequestEventHandle.newGroup";
	private final RedisCache redisCache;
	private final BotManager botManager;

	@Autowired
	public BotInvitedJoinGroupRequestEventHandle(RedisCache redisCache, BotManager botManager) {
		this.redisCache = redisCache;
		this.botManager = botManager;
	}

	@Override
	public void handleEvent(BotInvitedJoinGroupRequestEvent event) throws Exception {
		String message = String.format("被[%s][%s]邀请进入群[%s][%s]，是否接受(同意群邀请/拒绝群邀请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId());
		redisCache.setValue(newGroupKey, event);
		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
	}
}
