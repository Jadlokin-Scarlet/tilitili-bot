package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivCacheService;
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
    private final PixivCacheService pixivService;
    private final Map<String, String> keyMap = ImmutableMap.of(
            "ss", "r18",
            "st", "all",
            "色色", "r18",
            "色图", "all");

    @Autowired
    public PixivR18Handle(PixivCacheService pixivService) {
//        super("出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
        this.pixivService = pixivService;
    }

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws UnsupportedEncodingException, InterruptedException {
//        BotSender botSender = messageAction.getBotSender();
//        String pro = messageAction.getBodyOrDefault("pro", "0");
        String searchKey = messageAction.getValueOrDefault(messageAction.getBody("tag"));
        String user = messageAction.getBody("u");
        String source = messageAction.getBodyOrDefault("source", "pixiv");
        String num = messageAction.getBodyOrDefault("num", "1");
//        String sendMessageId = messageAction.getMessageId();
//        BotMessage botMessage = messageAction.getBotMessage();
        String titleKey = messageAction.getKeyWithoutPrefix();
        String r18 = keyMap.getOrDefault(titleKey, messageAction.getBodyOrDefault("r18", "2"));

        return pixivService.handlePixiv(messageAction, source, searchKey, user, r18, num);
    }

}
