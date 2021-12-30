package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.HelpHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String sendType = messageAction.getBotMessage().getSendType();
        String guildprefix = sendType.equals(SendTypeEmum.Guild_Message.sendType)? ".": "";

        MessageHandleEnum[] handleList = MessageHandleEnum.values();
        List<MessageHandleEnum> filterHandleList = Arrays.stream(handleList).filter(e -> e.getSendType().contains(sendType)).collect(Collectors.toList());

        StringBuilder stringBuilder = new StringBuilder("咱可以帮你做这些事！\n");

        String body = filterHandleList.stream().filter(a->!a.getDescription().isEmpty()).map(handle ->
                handle.getKeyword().stream().map(a -> guildprefix + a).collect(Collectors.joining(",")) + "：" + handle.getDescription()
        ).collect(Collectors.joining("\n"));
        return BotMessage.simpleTextMessage(stringBuilder.append(body).toString());
    }
}
