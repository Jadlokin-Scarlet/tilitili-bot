package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RecallEventHandle extends BaseEventHandleAdapt {
	private final BotSenderMapper botSenderMapper;
	private final BotMessageRecordMapper botMessageRecordMapper;
	private final BotManager botManager;
	private final BotSendMessageRecordMapper botSendMessageRecordMapper;

	@Autowired
	public RecallEventHandle(BotSenderMapper botSenderMapper, BotMessageRecordMapper botMessageRecordMapper, BotManager botManager, BotSendMessageRecordMapper botSendMessageRecordMapper) {
		super(BotEvent.EVENT_TYPE_RECALL);
		this.botSenderMapper = botSenderMapper;
		this.botMessageRecordMapper = botMessageRecordMapper;
		this.botManager = botManager;
		this.botSendMessageRecordMapper = botSendMessageRecordMapper;
	}

	@Override
	public BotMessage handleEvent(BotEnum bot, BotMessage botMessage) throws Exception {
		BotSender botSender = botMessage.getBotSender();
		String messageId = botMessage.getBotEvent().getMessageId();

		BotMessageRecord messageRecord = botMessageRecordMapper.getBotMessageRecordByMessageIdAndSenderId(messageId, botSender.getId());
		Asserts.notNull(messageRecord, "消息太久远啦");
		String replyMessageIdListStr = messageRecord.getReplyMessageId();
		Asserts.notNull(replyMessageIdListStr, "这条消息没有回复啦");

		List<String> replyMessageIdList = Arrays.asList(replyMessageIdListStr.split(","));
		for (String replyMessageId : replyMessageIdList) {
			BotSendMessageRecord replyMessage = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(replyMessageId);
			Asserts.notNull(replyMessage, "回复消息太久远啦");
			BotSender replySender = botSenderMapper.getValidBotSenderById(replyMessage.getSenderId());
			Asserts.notNull(replySender, "没有权限");

			botManager.recallMessage(BotEnum.getBotById(replySender.getBot()), replySender, replyMessageId);
		}
		return BotMessage.emptyMessage();
	}
}
