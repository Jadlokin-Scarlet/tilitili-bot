package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;

public interface BaseMessageHandle {
    MessageHandleEnum getType();
    MiraiMessage handleMessage(MiraiRequest request) throws Exception;
}
