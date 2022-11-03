package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.FindImageResult;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FindImageHandle extends ExceptionRespMessageHandle {
    private final PixivCacheService pixivService;
    private final BotMessageService botMessageService;

    @Autowired
    public FindImageHandle(PixivCacheService pixivService, BotMessageService botMessageService) {
        this.pixivService = pixivService;
        this.botMessageService = botMessageService;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String url = botMessageService.getFirstImageListOrQuoteImage(messageAction);
        FindImageResult findImageResult = pixivService.findImage(url);
        String link = findImageResult.getLink();
        String rate = findImageResult.getRate();
        String imageUrl = findImageResult.getImageUrl();
        return BotMessage.simpleImageTextMessage(String.format("找到啦😊！相似度%s%n%s", rate, link), imageUrl);
    }

}
