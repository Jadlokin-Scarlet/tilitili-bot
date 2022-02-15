package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.baidu.TranslateView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
        String from = messageAction.getParam("from");
        String to = messageAction.getParam("to");
        String isStatic = messageAction.getParamOrDefault("static", "0");

        String url = imageList.isEmpty()? "": imageList.get(0);
        String bodyNotNull = enText == null? "": enText;
        Asserts.notBlank(bodyNotNull + url, "格式错啦(内容)");

        String message;
        if (Objects.equals(isStatic, "1") && from != null && to != null) {
            message = BaiduManager.staticTranslate(from, to, enText);
        } else if (from != null) {
            message = baiduManager.translate(from, to, enText);
        } else if (to != null) {
            message = baiduManager.translate(to, enText);
        } else if (isNotBlank(enText)) {
            message = baiduManager.translate(enText);
        } else {
            TranslateView resultView = baiduManager.translateImage(url);
            message = String.format("%s\n---机翻\n%s", resultView.getSumSrc(), resultView.getSumDst());
        }
        if (isBlank(message)) {
            return BotMessage.simpleTextMessage("无法翻译");
        }
        return BotMessage.simpleTextMessage(message);
    }
}
