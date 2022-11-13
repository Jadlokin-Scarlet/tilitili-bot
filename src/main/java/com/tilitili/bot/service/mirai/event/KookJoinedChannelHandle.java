package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.KookAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.mirai.event.KookJoinedChannel;
import org.springframework.stereotype.Component;

@Component
public class KookJoinedChannelHandle extends KookAutoEventHandle<KookJoinedChannel> {
	public KookJoinedChannelHandle() {
		super(KookJoinedChannel.class);
	}

	@Override
	public void handleEvent(BotEmum bot, KookJoinedChannel event) throws Exception {
//		event.getD().getExtra().get
	}
}