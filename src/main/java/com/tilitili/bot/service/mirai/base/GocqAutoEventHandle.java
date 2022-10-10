package com.tilitili.bot.service.mirai.base;

import com.google.gson.reflect.TypeToken;
import com.tilitili.common.utils.Gsons;

public abstract class GocqAutoEventHandle<T> implements BaseEventHandle {
	private final TypeToken<?> type;

	public GocqAutoEventHandle(Class<T> clazz) {
		this.type = TypeToken.getParameterized(clazz);
	}

	@Override
	public void handleEventStr(String eventMessage) throws Exception {
		T event = Gsons.fromJson(eventMessage, type.getType());
		this.handleEvent(event);
	}

	public abstract void handleEvent(T event) throws Exception;
}
