package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FriendRequestEventHandle extends BaseEventHandleAdapt {
	public static final String newFriendKey = "FriendRequestEventHandle.newFriend";
	private final RedisCache redisCache;

	@Autowired
	public FriendRequestEventHandle(RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_FRIEND_REQUEST);
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleEvent(BotRobot bot, BotMessage botMessage) {
		BotSender botSender = botMessage.getBotSender();
		BotUserDTO botUser = botMessage.getBotUser();

		String message = String.format("[%s][%s]从[%s]申请加为好友，是否接受(同意好友邀请/拒绝好友邀请)", botUser.getName(), botUser.getQq(), botSender.getGroup());
		redisCache.setValue(newFriendKey, botMessage);
		return BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID);
	}
}
