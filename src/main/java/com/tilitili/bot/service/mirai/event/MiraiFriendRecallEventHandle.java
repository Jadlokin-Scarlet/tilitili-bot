package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.MiraiAutoEventHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotSendMessageRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.mirai.event.MiraiFriendRecallEvent;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSendMessageRecordMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MiraiFriendRecallEventHandle extends MiraiAutoEventHandle<MiraiFriendRecallEvent> {
	private final BotSenderMapper botSenderMapper;
	private final BotMessageRecordMapper botMessageRecordMapper;
	private final BotManager botManager;
	private final BotSendMessageRecordMapper botSendMessageRecordMapper;

	@Autowired
	public MiraiFriendRecallEventHandle(BotSenderMapper botSenderMapper, BotMessageRecordMapper botMessageRecordMapper, BotManager botManager, BotSendMessageRecordMapper botSendMessageRecordMapper) {
		super(MiraiFriendRecallEvent.class);
		this.botSenderMapper = botSenderMapper;
		this.botMessageRecordMapper = botMessageRecordMapper;
		this.botManager = botManager;
		this.botSendMessageRecordMapper = botSendMessageRecordMapper;
	}

	@Override
	public void handleEvent(BotEnum bot, MiraiFriendRecallEvent event) throws Exception {
		BotSender botSender = botSenderMapper.getValidBotSenderByQq(event.getAuthorId());
		Asserts.notNull(botSender, "无权限");
		Asserts.checkEquals(bot.id, botSender.getBot(), "没有权限");

		BotMessageRecord messageRecord = botMessageRecordMapper.getBotMessageRecordByMessageId(event.getMessageId());
		Asserts.notNull(messageRecord, "消息太久远啦");
		String replyMessageId = messageRecord.getReplyMessageId();
		Asserts.notNull(replyMessageId, "这条消息没有回复啦");
		BotSendMessageRecord replyMessage = botSendMessageRecordMapper.getNewBotSendMessageRecordByMessageId(replyMessageId);
		Asserts.notNull(replyMessage, "回复消息太久远啦");
		BotSender replySender = botSenderMapper.getValidBotSenderById(replyMessage.getSenderId());
		Asserts.notNull(replySender, "没有权限");

		botManager.recallMessage(BotEnum.getBotById(replySender.getBot()), replySender, replyMessageId);
	}
}
