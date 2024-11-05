package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson2.JSONObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class NailongRemoveHandle extends ExceptionRespMessageHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		List<String> imageList = messageAction.getImageList();
		for (String imageUrl : imageList) {
			String result = HttpClientUtil.httpPost("http://172.27.0.7:8081/check_image?image_url=" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8));
			log.info("check image url:{} result:{}", imageUrl, result);
			Asserts.notBlank(result, "网络异常");
			Boolean checkOk = JSONObject.parseObject(result).getBoolean("check_ok");
			log.info("check ok:{}", checkOk);
		}
		return null;
	}
}
