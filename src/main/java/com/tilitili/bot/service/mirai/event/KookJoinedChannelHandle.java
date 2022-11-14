package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.KookAutoEventHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.mirai.event.KookJoinedChannel;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KookJoinedChannelHandle extends KookAutoEventHandle<KookJoinedChannel> {
	public KookJoinedChannelHandle() {
		super(KookJoinedChannel.class);
	}

	@Override
	public void handleEvent(BotEmum bot, KookJoinedChannel event) throws Exception {
		log.info(Gsons.toJson(event));
//		event.getD().getExtra().get
	}
}