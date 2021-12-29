package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.HelpHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        StringBuilder stringBuilder = new StringBuilder("咱可以帮你做这些事！\n");
        String body = Arrays.stream(MessageHandleEnum.values()).filter(a->!a.getDescription().isEmpty()).map(handle ->
                String.join(",", handle.getKeyword()) + "：" + handle.getDescription()
        ).collect(Collectors.joining("\n"));
        return BotMessage.simpleTextMessage(stringBuilder.append(body).toString());
    }
}
