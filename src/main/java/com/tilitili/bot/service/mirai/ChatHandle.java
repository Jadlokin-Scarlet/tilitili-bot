package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;

@Slf4j
@Component
public class ChatHandle extends ExceptionRespMessageHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String req = messageAction.getParamOrDefault("提问", messageAction.getValue());
		Asserts.notBlank(req, "格式错啦(提问)");

		String respStr = HttpClientUtil.httpGet("http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + URLEncoder.encode(req, "UTF-8"));
		JSONObject resp = JSONObject.parseObject(respStr);
		log.debug("请求青云 req={} result={}", req, respStr);
		Asserts.checkEquals(resp.getInteger("result"), 0, "不对劲");

		return BotMessage.simpleTextMessage(resp.getString("content").replaceAll("\\{br}", "\n"));
	}
}
