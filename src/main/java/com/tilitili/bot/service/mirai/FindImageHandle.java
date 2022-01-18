package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FindImageHandle extends ExceptionRespMessageHandle {

	@Override
    public BotMessage handleMessage(BotMessageAction messageAction) {
        List<String> imageUrlList = messageAction.getImageList();
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
            Asserts.isTrue(Double.parseDouble(rateStr) > 80.0, "相似度过低\n"+url);
        }
        return BotMessage.simpleImageTextMessage(String.format("找到啦😊！相似度%s\n%s", rate, link), imageUrl);
    }
}
