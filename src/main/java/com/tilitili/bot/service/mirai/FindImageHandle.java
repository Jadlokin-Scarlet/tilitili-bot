package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.view.bot.mirai.MiraiMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class FindImageHandle implements BaseMessageHandle{

    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.FindImageHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) {
        MiraiMessage result = new MiraiMessage();
        String url = request.getUrl();
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
        return result.setMessage(String.format("找到啦😊！相似度%s\n%s", rate, link)).setUrl(imageUrl).setMessageType("ImageText");
    }
}
