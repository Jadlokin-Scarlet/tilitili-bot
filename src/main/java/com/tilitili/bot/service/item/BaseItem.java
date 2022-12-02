package com.tilitili.bot.service.item;

import com.tilitili.common.entity.BotItem;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;

public abstract class BaseItem {
	public abstract String getName();
	public abstract boolean useItem(BotSender botSender, BotUserDTO botUser, BotItem botItem);
}
