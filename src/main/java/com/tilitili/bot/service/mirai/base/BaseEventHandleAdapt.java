package com.tilitili.bot.service.mirai.base;

public abstract class BaseEventHandleAdapt implements BaseEventHandle {
	@Override
	public abstract void handleEventStr(String eventMessage) throws Exception;
}
