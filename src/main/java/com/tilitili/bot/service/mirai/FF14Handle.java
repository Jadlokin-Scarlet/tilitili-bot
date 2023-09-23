package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONPath;
import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.universalis.UniversalisItemPrice;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class FF14Handle extends ExceptionRespMessageHandle {
    private final Map<String, String> headers = ImmutableMap.of("Referer", "https://universalis.app/", "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.55 Safari/537.36", "cookie", "SL_G_WPT_TO=zh-CN; __Secure-next-auth.callback-url=https%3A%2F%2Funiversalis.app; __Host-next-auth.csrf-token=5013d4642b540a466e4b0c69eb2f13795ea4909accf8431b3c03fc5d57207fce%7Cb89bffe443901456e5487a6df11f45173ef59431b7f5183e839d64a92ea3c531; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1; mogboard_server=%E7%B4%AB%E6%B0%B4%E6%A0%88%E6%A1%A5; mogboard_language=chs; mogboard_timezone=Asia%2FShanghai; mogboard_leftnav=on; mogboard_homeworld=yes; mogboard_last_selected_server=%E7%B4%AB%E6%B0%B4%E6%A0%88%E6%A1%A5");

    @Override
    public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
        String value = messageAction.getValue();
        Integer id = this.search(value);
        if (id == null) {
            return null;
        }
        UniversalisItemPrice itemPrice = this.getPriceList(id);
        Asserts.notNull(itemPrice, "查询失败");
        return BotMessage.simpleTextMessage(String.format("%s的最低价位%d，更新于%s（包括 5%% 消费税）", value, itemPrice.getMinPrice(), formatDate(itemPrice.getLastUploadTime())));
    }

    JSONPath searchPath = JSONPath.of("$.Results[0].ID",  Integer.class);
    private Integer search(String name) throws UnsupportedEncodingException {
        String url = "https://cafemaker.wakingsands.com/search?string="+ URLEncoder.encode(name, "utf-8") +"&indexes=item&language=chs&filters=ItemSearchCategory.ID%3E=1&columns=ID,Icon,Name,LevelItem,Rarity,ItemSearchCategory.Name,ItemSearchCategory.ID,ItemKind.Name&limit=100&sort_field=LevelItem&sort_order=desc";
        String resp = HttpClientUtil.httpGet(url, headers);
        Asserts.notBlank(resp, "网络异常");
        try {
            Object id = searchPath.extract(resp);
            Asserts.notNull(id, "查无此物");
            return (Integer) id;
        } catch (JSONException e) {
            log.warn("error handle json="+resp, e);
            return null;
        }
    }

    JSONPath priceListPath = JSONPath.of("$.props.pageProps.markets.1043",  UniversalisItemPrice.class);
    private UniversalisItemPrice getPriceList(Integer id) {
        String html = HttpClientUtil.httpGet("https://universalis.app/market/" + id, headers);
        Asserts.notBlank(html, "网络异常");
        Document document = Jsoup.parse(html);

        String json = document.select("script#__NEXT_DATA__").first().data();
        Asserts.notBlank(json, "网络异常");
        try {
            return (UniversalisItemPrice) priceListPath.extract(json);
        } catch (JSONException e) {
            log.warn("error handle json="+json, e);
            return null;
        }
    }

    private String formatDate(Date inputDate) {
        // 获取当前时间
        Date currentTime = new Date();


        // 计算时间差（单位：毫秒）
        long timeDifference = currentTime.getTime() - inputDate.getTime();

        // 转换为秒、分钟、小时和天
        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        // 构建返回字符串
        String result = "";
        if (days > 0) {
            result = days + "天前";
        } else if (hours > 0) {
            result = hours + "小时前";
        } else if (minutes > 0) {
            result = minutes + "分钟前";
        } else {
            result = seconds + "秒前";
        }

        // 输出结果
        return result;
    }
}
