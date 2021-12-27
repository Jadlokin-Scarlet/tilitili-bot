package com.tilitili.bot.service.mirai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BeautifyJsonHandle implements BaseMessageHandle{
    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.BeautifyJsonHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();
        String body = request.getBody();
        Asserts.notBlank(body, "格式错啦(内容)");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return result.setMessage(gson.toJson(gson.fromJson(body, Map.class))).setMessageType("Plain");
    }
}
