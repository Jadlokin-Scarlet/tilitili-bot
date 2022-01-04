package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class HelpHandle extends ExceptionRespMessageHandle {
    private final List<BaseMessageHandle> handleList;

    @Autowired
    public HelpHandle(List<BaseMessageHandle> handleList) {
        this.handleList = handleList;
        handleList.add(this);
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.HelpHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String sendType = messageAction.getBotMessage().getSendType();
        String guildprefix = sendType.equals(SendTypeEmum.Guild_Message.sendType)? ".": "";

        // 筛选：不支持的发送方式不显示，未注册bean不显示，简介为空则不显示
        Stream<MessageHandleEnum> filterHandleList = handleList.stream().map(BaseMessageHandle::getType).filter(e -> e.getSendType().contains(sendType)).filter(e -> StringUtils.isNotBlank(e.getDescription()));

        Stream<String> lineList = filterHandleList.map(handle -> {
            String key = handle.getKeyword().stream().map(a -> guildprefix + a).collect(Collectors.joining(","));
            return String.format("%s：%s", key, handle.getDescription());
        });

        String head = "咱可以帮你做这些事！\n";
        String body = lineList.collect(Collectors.joining("\n"));

        return BotMessage.simpleTextMessage(head + body);
    }
}
