package com.tilitili.bot.service.mirai.base;

import com.google.gson.reflect.TypeToken;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.view.bot.mirai.MiraiBaseRequest;
import com.tilitili.common.utils.Gsons;

public abstract class MiraiAutoEventHandle<T> implements BaseEventHandle {
	private final TypeToken<?> type;

	public MiraiAutoEventHandle(Class<T> clazz) {
		this.type = TypeToken.getParameterized(MiraiBaseRequest.class, clazz);
	}

	@Override
	public void handleEventStr(BotEnum bot, String eventMessage) throws Exception {
		MiraiBaseRequest<T> request = Gsons.fromJson(eventMessage, type.getType());
		this.handleEvent(bot, request.getData());
	}

	public abstract void handleEvent(BotEnum bot, T event) throws Exception;
}
