package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.AutoEventHandle;
import com.tilitili.common.entity.view.bot.mirai.event.FriendInputStatusChangedEvent;
import com.tilitili.common.manager.BotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FriendInputStatusChangedEventHandle extends AutoEventHandle<FriendInputStatusChangedEvent> {
	private final BotManager botManager;

	@Autowired
	public FriendInputStatusChangedEventHandle(BotManager botManager) {
		super(FriendInputStatusChangedEvent.class);
		this.botManager = botManager;
	}

	@Override
	public void handleEvent(FriendInputStatusChangedEvent event) throws Exception {
//		Asserts.notNull(event.getFriend(), "似乎不是好友事件");
//		Asserts.isTrue(event.getInputting(), "离开事件不管");
//		botManager.sendMessage(BotMessage.simpleTextMessage("找我什么事鸭？").setSendType(SendTypeEmum.FRIEND_MESSAGE_STR).setQq(event.getFriend().getId()));
	}
}
