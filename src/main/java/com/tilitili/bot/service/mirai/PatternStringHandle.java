package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.util.TextUtils.isBlank;

@Component
public class PatternStringHandle extends ExceptionRespMessageHandle{

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.PATTERN_STRING_HANDLE;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String regex = messageAction.getParam("r");
        String string = messageAction.getParam("s");
        Asserts.notBlank(regex, "格式错啦(r)");
        Asserts.notBlank(string, "格式错啦(s)");
        List<String> pattenList = new ArrayList<>();
        pattenList.add(StringUtils.patten(regex, string));
        pattenList.addAll(StringUtils.extractList(regex, string));
        String patten = String.join("\n", pattenList);
        if (isBlank(patten)) {
            return BotMessage.simpleTextMessage("没匹配到");
        }
        return BotMessage.simpleTextMessage(patten);
    }
}
