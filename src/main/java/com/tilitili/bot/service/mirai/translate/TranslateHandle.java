package com.tilitili.bot.service.mirai.translate;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.baidu.TranslateView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.manager.BotTranslateMappingManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.BaiduUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Component
public class TranslateHandle extends ExceptionRespMessageHandle {

    private final BaiduManager baiduManager;
    private final BotTranslateMappingManager botTranslateMappingManager;

    @Autowired
    public TranslateHandle(BaiduManager baiduManager, BotTranslateMappingManager botTranslateMappingManager) {
        this.baiduManager = baiduManager;
        this.botTranslateMappingManager = botTranslateMappingManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        String enText = messageAction.getValueOrDefault(messageAction.getBody());
        List<String> imageList = messageAction.getImageList();
        String from = messageAction.getParam("from");
        String to = messageAction.getParam("to");

        String url = imageList.isEmpty()? "": imageList.get(0);
        String bodyNotNull = enText == null? "": enText;
        Asserts.notBlank(bodyNotNull + url, "格式错啦(内容)");

        String message;
        if (from != null) {
            message = botTranslateMappingManager.translate(botSender.getId(), enText, from, to);
        } else if (to != null) {
            message = botTranslateMappingManager.translate(botSender.getId(), enText, to);
        } else if (isNotBlank(enText)) {
            message = botTranslateMappingManager.translate(botSender.getId(), enText);
        } else {
            TranslateView resultView = baiduManager.translateImage(url);
            message = String.format("%s%n---机翻%n%s", resultView.getSumSrc(), resultView.getSumDst());
        }
        if (isBlank(message)) {
            return BotMessage.simpleTextMessage("无法翻译");
        }
        return BotMessage.simpleTextMessage(message);
    }
}
