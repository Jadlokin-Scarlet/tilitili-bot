package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiFriendInputStatusChangedEvent;
import com.tilitili.common.manager.BotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MiraiFriendInputStatusChangedEventHandle extends MiraiAutoEventHandle<MiraiFriendInputStatusChangedEvent> {
	private final BotManager botManager;

	@Autowired
	public MiraiFriendInputStatusChangedEventHandle(BotManager botManager) {
		super(MiraiFriendInputStatusChangedEvent.class);
		this.botManager = botManager;
	}

	@Override
	public void handleEvent(BotEnum bot, MiraiFriendInputStatusChangedEvent event) throws Exception {
//		Asserts.notNull(event.getFriend(), "似乎不是好友事件");
//		Asserts.isTrue(event.getInputting(), "离开事件不管");
//		botManager.sendMessage(BotMessage.simpleTextMessage("找我什么事鸭？").setSendType(SendTypeEnum.FRIEND_MESSAGE_STR).setQq(event.getFriend().getId()));
	}
}
