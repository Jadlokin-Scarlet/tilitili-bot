package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class PixivHandle extends ExceptionRespMessageHandle {
    public static final String messageIdKey = "pixiv.messageId";
    private final PixivService pixivService;

    @Autowired
    public PixivHandle(PixivService pixivService) {
//        super("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
        this.pixivService = pixivService;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
        BotSender botSender = messageAction.getBotSender();
        String pro = messageAction.getParamOrDefault("pro", "0");
        String searchKey = messageAction.getValueOrDefault(messageAction.getParam("tag"));
        String user = messageAction.getParam("u");
        String source = messageAction.getParamOrDefault("source", "pixiv");
        String num = messageAction.getParamOrDefault("num", "1");
        String sendMessageId = messageAction.getMessageId();
        BotMessage botMessage = messageAction.getBotMessage();
        String r18 = "0";

        pixivService.handlePixiv(botMessage, sendMessageId, source, searchKey, user, r18, num, botSender);
        return BotMessage.emptyMessage();
    }

}
