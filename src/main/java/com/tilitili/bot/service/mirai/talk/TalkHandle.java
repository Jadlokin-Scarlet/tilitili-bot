package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;

	@Autowired
	public TalkHandle(BotTalkMapper botTalkMapper) {
		this.botTalkMapper = botTalkMapper;
	}

	@Override
	public MessageHandleEnum getType() {
		return MessageHandleEnum.TALK_HANDLE;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		String sendType = botMessage.getSendType();
		Long qq = botMessage.getQq();
		Long group = botMessage.getGroup();
		String guildId = botMessage.getGuildId();
		String channelId = botMessage.getChannelId();
		String tinyId = botMessage.getTinyId();

		String value = messageAction.getValueOrDefault("");
		String req = messageAction.getBodyOrDefault("提问", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): null);
		String resp = messageAction.getBodyOrDefault("回答", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		BotTalk addBotTalk = new BotTalk().setReq(req).setResp(resp).setSendType(sendType).setSendQq(qq).setSendGroup(group).setSendGuild(guildId).setSendChannel(channelId).setSendTiny(tinyId);
		botTalkMapper.addBotTalkSelective(addBotTalk);
		return BotMessage.simpleTextMessage("学废了！");
	}
}
