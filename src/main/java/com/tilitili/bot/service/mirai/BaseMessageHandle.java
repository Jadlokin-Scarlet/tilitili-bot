package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;
import com.tilitili.common.exception.AssertException;

public interface BaseMessageHandle {
    MessageHandleEnum getType();
    BotMessage handleMessage(BotMessageAction messageAction) throws Exception;
    BotMessage handleAssertException(BotMessageAction messageAction, AssertException e);
}
