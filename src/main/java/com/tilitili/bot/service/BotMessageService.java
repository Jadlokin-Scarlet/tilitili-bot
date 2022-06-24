package com.tilitili.bot.service;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotMessageService {

	public String getQuotePid(BotMessageAction messageAction) {
		if (messageAction.getQuoteMessage() != null) {
			BotMessage quoteMessage = messageAction.getQuoteMessage();
			BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null);
			String text = quoteMessageAction.getText();
			return StringUtils.patten1("https://pixiv.moe/illust/(\\d+)", text);
		}
		return null;
	}

	public List<String> getImageListOrQuoteImage(BotMessageAction messageAction) {
		List<String> imageUrlList = messageAction.getImageList();

		if (CollectionUtils.isEmpty(imageUrlList) && messageAction.getQuoteMessage() != null) {
			BotMessage quoteMessage = messageAction.getQuoteMessage();
			BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null);
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

	public boolean isAdmin(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		String sendType = botMessage.getSendType();
		Asserts.notNull(sendType, "找不到发送渠道");
		switch (sendType) {
			case SendTypeEmum.FRIEND_MESSAGE_STR: return true;
			case SendTypeEmum.GROUP_MESSAGE_STR: return isGroupAdmin(messageAction);
			case SendTypeEmum.GUILD_MESSAGE_STR: return isGuildAdmin(messageAction);
			default: return false;
		}
	}

	private boolean isGuildAdmin(BotMessageAction messageAction) {
		return false;
	}

	private boolean isGroupAdmin(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
//		botManager.getMemberProfile();
		return false;
	}
}
