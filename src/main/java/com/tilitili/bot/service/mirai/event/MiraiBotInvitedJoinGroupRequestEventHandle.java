package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiBotInvitedJoinGroupRequestEvent;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiraiBotInvitedJoinGroupRequestEventHandle extends MiraiAutoEventHandle<MiraiBotInvitedJoinGroupRequestEvent> {
	public static final String newGroupKey = "MiraiBotInvitedJoinGroupRequestEventHandle.newGroup";
	private final RedisCache redisCache;
	private final SendMessageManager sendMessageManager;

	@Autowired
	public MiraiBotInvitedJoinGroupRequestEventHandle(RedisCache redisCache, SendMessageManager sendMessageManager) {
		super(MiraiBotInvitedJoinGroupRequestEvent.class);
		this.redisCache = redisCache;
		this.sendMessageManager = sendMessageManager;
	}

	@Override
	public void handleEvent(BotEmum bot, MiraiBotInvitedJoinGroupRequestEvent event) throws Exception {
		String message = String.format("被[%s][%s]邀请进入群[%s][%s]，是否接受(同意群邀请/拒绝群邀请)", event.getNick(), event.getFromId(), event.getGroupName(), event.getGroupId());
		redisCache.setValue(newGroupKey, event);
		sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
	}
}
