package com.tilitili.bot.service.mirai.base;

import com.google.gson.reflect.TypeToken;
import com.tilitili.common.entity.view.bot.mirai.MiraiBaseRequest;
import com.tilitili.common.utils.Gsons;

public abstract class AutoEventHandle<T> implements BaseEventHandle {
	@Override
	public void handleEventStr(String eventMessage) throws Exception {
		MiraiBaseRequest<T> request = Gsons.fromJson(eventMessage, new TypeToken<MiraiBaseRequest<T>>(){}.getType());
		handleEvent(request.getData());
	}

	public abstract void handleEvent(T event) throws Exception;
}
