package com.tilitili.bot.service.mirai.translate;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.baidu.TranslateView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.manager.BotTranslateMappingManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Component
public class TranslateHandle extends ExceptionRespMessageHandle {
    private final BotMessageService botMessageService;
    private final BaiduManager baiduManager;
    private final BotTranslateMappingManager botTranslateMappingManager;

    @Autowired
    public TranslateHandle(BotMessageService botMessageService, BaiduManager baiduManager, BotTranslateMappingManager botTranslateMappingManager) {
        this.botMessageService = botMessageService;
        this.baiduManager = baiduManager;
        this.botTranslateMappingManager = botTranslateMappingManager;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        BotSender botSender = messageAction.getBotSender();
        String enText = messageAction.getValueOrDefault(messageAction.getBody());
        if (StringUtils.isBlank(enText) && messageAction.getQuoteMessage() != null) {
            BotMessage quoteMessage = messageAction.getQuoteMessage();
            enText = quoteMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_PLAIN)).map(BotMessageChain::getText).collect(Collectors.joining(""));
        }
        List<String> imageList = botMessageService.getImageListOrQuoteImage(messageAction);
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
            message = resultView != null ? String.format("%s%n---机翻%n%s", resultView.getSumSrc(), resultView.getSumDst()) : null;
        }
        if (isBlank(message)) {
            return BotMessage.simpleTextMessage("无法翻译");
        }
        return BotMessage.simpleTextMessage(message);
    }
}
