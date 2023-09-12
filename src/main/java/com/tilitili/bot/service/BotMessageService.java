package com.tilitili.bot.service;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.CollectionUtils;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotMessageService {

	public String getQuotePid(BotMessageAction messageAction) {
		if (messageAction.getQuoteMessage() != null) {
			BotMessage quoteMessage = messageAction.getQuoteMessage();
			BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null, null);
			String text = quoteMessageAction.getText();
			String pid = StringUtils.patten1("pid: (\\d+)", text);
			if (StringUtils.isBlank(pid) && text.contains("https://www.pixiv.net")) {
				pid = StringUtils.patten1("&illust_id=(\\d+)", text);
			}
			if (StringUtils.isBlank(pid) && text.contains("https://pixiv.nl")) {
				pid = StringUtils.patten1("/(\\d+)", text);
			}
			return pid;
		}
		return null;
	}

	public List<String> getImageListOrQuoteImage(BotMessageAction messageAction) {
		List<String> imageUrlList = messageAction.getImageList();

		if (CollectionUtils.isEmpty(imageUrlList) && messageAction.getQuoteMessage() != null) {
			BotMessage quoteMessage = messageAction.getQuoteMessage();
			BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null, null);
			imageUrlList = quoteMessageAction.getImageList();
		}

		return imageUrlList;
	}

	public String getFirstImageListOrQuoteImage(BotMessageAction messageAction) {
		List<String> imageUrlList = this.getImageListOrQuoteImage(messageAction);

		Asserts.notEmpty(imageUrlList, "格式错啦(图片)");
		String url = QQUtil.getImageUrl(imageUrlList.get(0));
		Asserts.notBlank(url, "格式错啦(图片)");
		return url;
	}
}
