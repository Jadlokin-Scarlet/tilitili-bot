package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class NoBakaHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.NoBakaHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        String text = messageAction.getText();
        Long qq = messageAction.getBotMessage().getQq();

        int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
        if (ddCount > 0) {
            String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
            return BotMessage.simpleTextMessage(repeat);
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        if (text.equals("让我看看") || text.equals("让我康康")) {
            return BotMessage.simpleTextMessage("不要！");
        }

        return null;
    }
}
