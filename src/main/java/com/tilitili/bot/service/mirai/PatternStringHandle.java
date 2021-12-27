package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.mirai.MiraiMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.util.TextUtils.isBlank;

@Component
public class PatternStringHandle implements BaseMessageHandle{

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.PatternStringHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();
        String regex = request.getParam("r");
        String string = request.getParam("s");
        Asserts.notBlank(regex, "格式错啦(r)");
        Asserts.notBlank(string, "格式错啦(s)");
        List<String> pattenList = new ArrayList<>();
        pattenList.add(StringUtils.patten(regex, string));
        pattenList.addAll(StringUtils.extractList(regex, string));
        String patten = String.join("\n", pattenList);
        if (isBlank(patten)) {
            return result.setMessage("没匹配到").setMessageType("Plain");
        }
        return result.setMessage(patten).setMessageType("Plain");
    }
}
