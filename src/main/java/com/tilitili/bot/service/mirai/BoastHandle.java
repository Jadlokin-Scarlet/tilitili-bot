package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONPath;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoastHandle extends ExceptionRespMessageHandle {
	@Value("${mirai.bot-qq}")
	private String BOT_QQ;
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		List<Long> atList = messageAction.getAtList();
		if (!atList.contains(Long.valueOf(BOT_QQ))) {
			return null;
		}
		String result = HttpClientUtil.httpGet("https://api.shadiao.app/chp");
		Asserts.notBlank(result, "网络异常");
		String text = JSONPath.read(result, "$.data.text", String.class);
		Asserts.notBlank(text, "网络异常");
		return BotMessage.simpleTextMessage(text);
	}
}
