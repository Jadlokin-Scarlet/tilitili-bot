package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
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
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String enText = messageAction.getValueOrDefault(messageAction.getBody());
        List<String> imageList = messageAction.getImageList();
        String to = messageAction.getParam("to");

        String url = imageList.isEmpty()? "": imageList.get(0);
        String bodyNotNull = enText == null? "": enText;
        Asserts.notBlank(bodyNotNull + url, "格式错啦(内容)");

        String cnText;
        if (to != null) {
            cnText = baiduManager.translate(to, enText);
        } else if (isNotBlank(enText)) {
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
