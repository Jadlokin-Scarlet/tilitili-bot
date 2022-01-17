package com.tilitili.bot.service.mirai.base;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;

public interface BaseMessageHandle {
    BotMessage handleMessage(BotMessageAction messageAction) throws Exception;
    BotMessage handleAssertException(BotMessageAction messageAction, AssertException e);
}
