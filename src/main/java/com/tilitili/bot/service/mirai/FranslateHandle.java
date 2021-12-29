package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Component
public class FranslateHandle extends ExceptionRespMessageHandle {

    private final BaiduManager baiduManager;

    @Autowired
    public FranslateHandle(BaiduManager baiduManager) {
        this.baiduManager = baiduManager;
    }

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.FranslateHandle;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String body = messageAction.getBody();
        List<String> imageList = messageAction.getImageList();
        String to = messageAction.getParam("to");
        String text = messageAction.getParamOrDefault("t", "");

        String url = imageList.isEmpty()? "": imageList.get(0);
        Asserts.notBlank(body + url + text, "格式错啦(内容)");

        String enText = (text + body);
        String cnText;
        if (to != null) {
            cnText = baiduManager.translate(to, enText);
        } else if (isNotBlank(body)) {
            cnText = baiduManager.translate(enText);
        } else {
            cnText = baiduManager.translateImage(url);
        }
        if (isBlank(cnText)) {
            return BotMessage.simpleTextMessage("无法翻译");
        }
        return BotMessage.simpleTextMessage(cnText);
    }
}
