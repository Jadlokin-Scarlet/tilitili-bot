package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MemberJoinRequestEvent;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberJoinRequestEventHandle extends AutoEventHandle<MemberJoinRequestEvent> {
	public static final String newMemberKey = "MemberJoinRequestEvent.newMember";
	private final RedisCache redisCache;
	private final BotManager botManager;

	@Autowired
	public MemberJoinRequestEventHandle(BotManager botManager, RedisCache redisCache) {
		super(MemberJoinRequestEvent.class);
		this.botManager = botManager;
		this.redisCache = redisCache;
	}

	@Override
	public void handleEvent(MemberJoinRequestEvent event) {
		String message = String.format("[%s][%s]申请加入[%s][%s]，是否接受(同意加群申请/拒绝加群申请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId());
		redisCache.setValue(newMemberKey, event);
		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
	}
}
