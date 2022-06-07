package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.utils.*;
import org.apache.commons.collections.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FindImageHandle extends ExceptionRespMessageHandle {
    @Value("${mirai.bot-qq}")
    private String BOT_QQ;
    private final Gson gson;
    private final BotManager botManager;
    private final BotSendMessageRecordMapper botSendMessageRecordMapper;

    @Autowired
    public FindImageHandle(BotManager botManager, BotSendMessageRecordMapper botSendMessageRecordMapper) {
        gson = new Gson();
        this.botManager = botManager;
        this.botSendMessageRecordMapper = botSendMessageRecordMapper;
    }

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        String quoteMessageId = messageAction.getQuoteMessageId();
        Long quoteSenderId = messageAction.getQuoteSenderId();
        List<String> imageUrlList = messageAction.getImageList();

        if (CollectionUtils.isEmpty(imageUrlList) && quoteMessageId != null) {
            if (Objects.equals(String.valueOf(quoteSenderId), BOT_QQ)) {
                BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
                BotMessage quoteMessage = gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
                imageUrlList = quoteMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Image")).map(BotMessageChain::getUrl).collect(Collectors.toList());
            } else {
                BotMessageRecord quoteMessageRecord = botManager.getMessage(quoteMessageId);
                BotMessage quoteMessage = botManager.handleMessageRecordToBotMessage(quoteMessageRecord);
                BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null);
                imageUrlList = quoteMessageAction.getImageList();
            }
        }

        Asserts.notEmpty(imageUrlList, "格式错啦(图片)");
        String url = QQUtil.getImageUrl(imageUrlList.get(0));
        Asserts.notBlank(url, "格式错啦(图片)");
        String html = HttpClientUtil.httpPost("https://saucenao.com/search.php?url="+url, ImmutableMap.of());
        Asserts.notBlank(html, "没要到图😇\n"+url);
        Document document = Jsoup.parse(html);
        Elements imageList = document.select(".result:not(.hidden):not(#result-hidden-notification)");
        Asserts.isFalse(imageList.isEmpty(), "没找到🤕\n"+url);
        Element image = imageList.get(0);

        String rate = image.select(".resultsimilarityinfo").text();
        String imageUrl = image.select(".resulttableimage img").attr("src");
        Elements linkList = image.select(".resultcontentcolumn a.linkify");
        Asserts.notBlank(rate, "没找到😑\n"+url);
        Asserts.notBlank(imageUrl, "没找到😑\n"+url);
        Asserts.isFalse(linkList.isEmpty(), "没找到😑\n"+url);

        String link = linkList.get(0).attr("href");
        String rateStr = rate.replace("%", "");
        if (StringUtils.isNumber(rateStr)) {
            Asserts.isTrue(Double.parseDouble(rateStr) > 60.0, "相似度过低(怪图警告)\n"+link);
            Asserts.isTrue(Double.parseDouble(rateStr) > 80.0, "相似度过低\n"+link);
        }
        return BotMessage.simpleImageTextMessage(String.format("找到啦😊！相似度%s%n%s", rate, link), imageUrl);
    }
}
