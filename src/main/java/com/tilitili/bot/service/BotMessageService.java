package com.tilitili.bot.service;

import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StreamUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BotMessageService {
	@Value("${mirai.bot-qq}")
	private String BOT_QQ;
	private final Gson gson;
	private final BotManager botManager;
	private final BotSendMessageRecordMapper botSendMessageRecordMapper;

	public BotMessageService(BotManager botManager, BotSendMessageRecordMapper botSendMessageRecordMapper) {
		this.gson = new Gson();
		this.botManager = botManager;
		this.botSendMessageRecordMapper = botSendMessageRecordMapper;
	}


	public String getFirstImageListOrQuoteImage(BotMessageAction messageAction) {
		List<String> imageUrlList = this.getImageListOrQuoteImageList(messageAction);

		Asserts.notEmpty(imageUrlList, "格式错啦(图片)");
		String url = QQUtil.getImageUrl(imageUrlList.get(0));
		Asserts.notBlank(url, "格式错啦(图片)");
		return url;
	}

	public List<String> getImageListOrQuoteImageList(BotMessageAction messageAction) {
		String quoteMessageId = messageAction.getQuoteMessageId();
		Long quoteSenderId = messageAction.getQuoteSenderId();
		List<String> imageUrlList = messageAction.getImageList();

		if (CollectionUtils.isEmpty(imageUrlList) && quoteMessageId != null) {
			if (Objects.equals(String.valueOf(quoteSenderId), BOT_QQ)) {
				BotSendMessageRecord sendMessageRecord = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(quoteMessageId);
				BotMessage quoteMessage = gson.fromJson(sendMessageRecord.getMessage(), BotMessage.class);
				imageUrlList = quoteMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, "Image")).map(BotMessageChain::getUrl).collect(Collectors.toList());
			} else {
				BotMessageRecord quoteMessageRecord = botManager.getMessage(quoteMessageId);
				BotMessage quoteMessage = botManager.handleMessageRecordToBotMessage(quoteMessageRecord);
				BotMessageAction quoteMessageAction = new BotMessageAction(quoteMessage, null);
				imageUrlList = quoteMessageAction.getImageList();
			}
		}
		return imageUrlList;
	}
}
