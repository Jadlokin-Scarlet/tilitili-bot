package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiNewFriendRequestEvent;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiraiNewFriendRequestEventHandle extends MiraiAutoEventHandle<MiraiNewFriendRequestEvent> {
	public static final String newFriendKey = "MiraiNewFriendRequestEventHandle.newFriend";
	private final RedisCache redisCache;
	private final BotManager botManager;

	@Autowired
	public MiraiNewFriendRequestEventHandle(BotManager botManager, RedisCache redisCache) {
		super(MiraiNewFriendRequestEvent.class);
		this.botManager = botManager;
		this.redisCache = redisCache;
	}

	@Override
	public void handleEvent(BotEmum bot, MiraiNewFriendRequestEvent event) throws Exception {
		String message = String.format("[%s][%s]从[%s]申请加为好友，是否接受(同意好友邀请/拒绝好友邀请)", event.getNick(), event.getFromId(), event.getGroupId());
		redisCache.setValue(newFriendKey, event);
		botManager.sendMessage(BotMessage.simpleTextMessage(message).setSenderId(BotSenderConstant.MASTER_SENDER_ID));
	}
}
