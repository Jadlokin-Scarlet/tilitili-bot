package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.query.BotTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteTalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;

	@Autowired
	public DeleteTalkHandle(BotTalkMapper botTalkMapper) {
		this.botTalkMapper = botTalkMapper;
	}

	@Override
	public MessageHandleEnum getType() {
		return MessageHandleEnum.DELETE_TALK_HANDLE;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String req = messageAction.getBodyOrDefault("提问", messageAction.getValue());
		BotMessage botMessage = messageAction.getBotMessage();
		String sendType = botMessage.getSendType();
		Long qq = botMessage.getQq();
		Long group = botMessage.getGroup();
		String guildId = botMessage.getGuildId();
		String channelId = botMessage.getChannelId();

		BotTalkQuery botTalkQuery = new BotTalkQuery().setReq(req).setSendType(sendType);
		switch (sendType) {
			case SendTypeEmum.FRIEND_MESSAGE: botMessage.setQq(qq); break;
			case SendTypeEmum.GROUP_MESSAGE: botMessage.setGroup(group); break;
			case SendTypeEmum.TEMP_MESSAGE: botMessage.setQq(qq).setGroup(group); break;
			case SendTypeEmum.GUILD_MESSAGE: botMessage.setGuildId(guildId).setChannelId(channelId); break;
		}
		List<BotTalk> botTalkList = botTalkMapper.getBotTalkByCondition(botTalkQuery);
		Asserts.notEmpty(botTalkList, "没有找到对话。");
		Asserts.notEquals(botTalkList.size(), 1, "不对劲。");

		BotTalk botTalk = botTalkList.get(0);
		botTalkMapper.updateBotTalkSelective(new BotTalk().setId(botTalk.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("移除了。");
	}
}
