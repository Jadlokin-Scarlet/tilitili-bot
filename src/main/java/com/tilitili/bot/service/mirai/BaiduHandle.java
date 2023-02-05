package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class BaiduHandle extends BaseMessageHandleAdapt {

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String value = messageAction.getValue();
		Asserts.notBlank(value, "格式错啦(内容)");

		String url = "https://www.baidu.com/s?ie=UTF-8&wd=" + URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		if (url.length() > 100) {
			url = "https://www.baidu.com/s?ie=UTF-8&wd=" + value;
		}

		return BotMessage.simpleLinkMessage(url);
	}
}
