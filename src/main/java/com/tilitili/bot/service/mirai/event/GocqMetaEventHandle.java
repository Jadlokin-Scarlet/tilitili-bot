package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.GocqAutoEventHandle;
import com.tilitili.common.entity.view.bot.gocqhttp.GocqhttpBaseEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GocqMetaEventHandle extends GocqAutoEventHandle<GocqhttpBaseEvent> {

	@Autowired
	public GocqMetaEventHandle() {
		super(GocqhttpBaseEvent.class);
	}

	@Override
	public void handleEvent(GocqhttpBaseEvent event) throws Exception {
	}
}