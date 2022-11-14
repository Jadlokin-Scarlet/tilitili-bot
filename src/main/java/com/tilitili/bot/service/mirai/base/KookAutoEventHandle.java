package com.tilitili.bot.service.mirai.base;

import com.google.gson.reflect.TypeToken;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.kook.KookEventResponse;
import com.tilitili.common.utils.Gsons;

public abstract class KookAutoEventHandle<T> implements BaseEventHandle {
	private final TypeToken<?> type;

	public KookAutoEventHandle(Class<T> clazz) {
		this.type = TypeToken.getParameterized(KookEventResponse.class, clazz);
	}

	@Override
	public void handleEventStr(BotEmum bot, String eventMessage) throws Exception {
		KookEventResponse<T> resp = Gsons.fromJson(eventMessage, type.getType());
		this.handleEvent(bot, resp.getData().getExtra().getBody());
	}

	public abstract void handleEvent(BotEmum bot, T event) throws Exception;
}
