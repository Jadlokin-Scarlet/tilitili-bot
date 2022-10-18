package com.tilitili.bot.service.mirai.base;

import com.tilitili.common.emnus.BotEmum;

public abstract class BaseEventHandleAdapt implements BaseEventHandle {
	@Override
	public abstract void handleEventStr(BotEmum bot, String eventMessage) throws Exception;
}
