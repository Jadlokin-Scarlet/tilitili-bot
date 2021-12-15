package com.tilitili.bot.service.mirai;

import com.google.gson.Gson;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.cfcx.CfcxData;
import com.tilitili.bot.entity.cfcx.CfcxItem;
import com.tilitili.bot.entity.cfcx.CfcxResponse;
import com.tilitili.bot.entity.mirai.MiraiRequest;
import com.tilitili.common.entity.mirai.MiraiMessage;
import com.tilitili.common.entity.mirai.Sender;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.DateUtils;
import com.tilitili.common.utils.HttpClientUtil;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Collectors;

@Component
public class CfcxHandle implements BaseMessageHandle {


    @Override
    public MessageHandleEnum getType() {
        return MessageHandleEnum.CfcxHandle;
    }

    @Override
    public MiraiMessage handleMessage(MiraiRequest request) throws Exception {
        MiraiMessage result = new MiraiMessage();
        Long sendMessageId = Long.valueOf(request.getMessageId());

        String titleValue = request.getTitleValue();
        if (TextUtils.isBlank(titleValue)) {
            return null;
        }

        String url = "https://api.asoulfan.com/cfj/?name="+titleValue;
        String jsonStr = HttpClientUtil.httpGet(url);
        Asserts.notBlank(jsonStr, "找不了了。");
        CfcxResponse cfcxResponse = new Gson().fromJson(jsonStr, CfcxResponse.class);
        Asserts.notNull(cfcxResponse, "看不懂了");
        Asserts.checkEquals(cfcxResponse.getCode(), 0, "找不到了");
        CfcxData data = cfcxResponse.getData();
        String nameListStr = data.getList().stream().map(CfcxItem::getUname).collect(Collectors.joining(","));
        String message = String.format("关注的VUP有：\n%s\n查询时间：%s\n数据来源：ProJectASF", nameListStr, DateUtils.formatDateYMDHMS(new Date()));

        return result.setMessage(message).setMessageType("Plain").setQuote(sendMessageId);
    }
}
