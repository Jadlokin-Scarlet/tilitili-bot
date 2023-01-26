package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvitedJoinGroupEventHandle extends BaseEventHandleAdapt {
	public static final String newGroupKey = "InvitedJoinGroupEventHandle.newGroup";
	private final RedisCache redisCache;

	@Autowired
	public InvitedJoinGroupEventHandle(RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_INVITED_JOIN_GROUP);
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleEvent(BotEnum bot, BotMessage botMessage) throws Exception {
		BotSender botSender = botMessage.getBotSender();
		BotUserDTO botUser = botMessage.getBotUser();

		String message = String.format("被[%s][%s]邀请进入群[%s][%s]，是否接受(同意群邀请/拒绝群邀请)", botUser.getName(), botUser.getQq(), botSender.getName(), botSender.getGroup());
		redisCache.setValue(newGroupKey, botMessage);
		return BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID);
	}
}
