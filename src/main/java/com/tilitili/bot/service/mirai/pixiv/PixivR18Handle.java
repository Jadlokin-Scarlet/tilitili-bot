package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Slf4j
@Component
public class PixivR18Handle extends ExceptionRespMessageHandle {
    private final PixivService pixivService;
    private final Map<String, String> keyMap = ImmutableMap.of("ss", "1", "st", "2");

    @Autowired
    public PixivR18Handle(PixivService pixivService) {
//        super("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
        this.pixivService = pixivService;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
        String pro = messageAction.getParamOrDefault("pro", "0");
        String searchKey = messageAction.getValueOrDefault(messageAction.getParam("tag"));
        String user = messageAction.getParam("u");
        String source = messageAction.getParamOrDefault("source", "pixiv");
        String num = messageAction.getParamOrDefault("num", "1");
        String sendMessageId = messageAction.getMessageId();
        BotMessage botMessage = messageAction.getBotMessage();
        String titleKey = messageAction.getKeyWithoutPrefix();
        String r18 = keyMap.getOrDefault(titleKey, messageAction.getParamOrDefault("r18", "2"));

        pixivService.handlePixiv(botMessage, sendMessageId, source, searchKey, user, r18, num, pro);
        return BotMessage.emptyMessage();
    }

}
