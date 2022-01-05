package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReplyHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.master-qq}")
    private Long MASTER_QQ;
    private final BotTalkManager botTalkManager;

    @Autowired
    public ReplyHandle(BotTalkManager botTalkManager) {
        this.botTalkManager = botTalkManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.REPLY_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String text = messageAction.getText();
        BotMessage botMessage = messageAction.getBotMessage();
        Long qq = botMessage.getQq();
        List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(text, botMessage);
        if (!botTalkList.isEmpty()) {
            BotTalk botTalk = botTalkList.get(0);
            return BotMessage.simpleTextMessage(botTalk.getResp());
        }

        int ddCount = StringUtils.findCount("dd|DD|dD|Dd", text);
        if (ddCount > 0) {
            String repeat = IntStream.range(0, ddCount).mapToObj(c -> "bd").collect(Collectors.joining());
            return BotMessage.simpleTextMessage(repeat);
        }

        if (Objects.equals(qq, MASTER_QQ) && text.equals("cww")) {
            return BotMessage.simpleTextMessage("cww");
        }

        return null;
    }
}
