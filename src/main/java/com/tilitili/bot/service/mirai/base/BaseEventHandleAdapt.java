package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEnum;

public abstract class BaseEventHandleAdapt implements BaseEventHandle {
	@Override
	public abstract void handleEventStr(BotEnum bot, String eventMessage) throws Exception;
}
