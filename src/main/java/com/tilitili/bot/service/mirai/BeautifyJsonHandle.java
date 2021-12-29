package com.tilitili.bot.service.mirai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BeautifyJsonHandle extends ExceptionRespMessageHandle{
    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.BeautifyJsonHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String body = messageAction.getBody();
        Asserts.notBlank(body, "格式错啦(内容)");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return BotMessage.simpleTextMessage(gson.toJson(gson.fromJson(body, Map.class)));
    }
}
