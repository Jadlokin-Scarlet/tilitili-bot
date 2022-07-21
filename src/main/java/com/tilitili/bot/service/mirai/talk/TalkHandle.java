package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;

	@Autowired
	public TalkHandle(BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
		this.botTalkMapper = botTalkMapper;
		this.botTalkManager = botTalkManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotMessage botMessage = messageAction.getBotMessage();
		List<String> imageList = messageAction.getImageList();
		String sendType = botMessage.getSendType();
		Long qq = botMessage.getQq();
		Long group = botMessage.getGroup();
		Long guildId = botMessage.getGuildId();
		Long channelId = botMessage.getChannelId();
		Long tinyId = botMessage.getTinyId();

		String value = messageAction.getValueOrDefault("");
		String req = messageAction.getBodyOrDefault("提问", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): null);
		String resp = messageAction.getBodyOrDefault("回答", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		int type;
		if (StringUtils.isBlank(req) && StringUtils.isBlank(resp) && imageList.size() == 2) {
			req = QQUtil.getImageUrl(imageList.get(0));
			resp = QQUtil.getImageUrl(imageList.get(1));
			type = 1;
		} else {
			Asserts.notBlank(req, "格式不对(提问)");
			Asserts.notBlank(resp, "格式不对(回答)");
			type = 0;
		}

		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, messageAction.getBotMessage());
		botTalkList.forEach(botTalk -> botTalkMapper.deleteBotTalkByPrimary(botTalk.getId()));
		String respList = botTalkList.stream().map(BotTalk::getResp).collect(Collectors.joining(" 或 "));

		BotTalk addBotTalk = new BotTalk().setType(type).setReq(req).setResp(resp).setSendType(sendType).setSendQq(qq).setSendGroup(group).setSendGuild(guildId).setSendChannel(channelId).setSendTiny(tinyId);
		botTalkMapper.addBotTalkSelective(addBotTalk);
		if (botTalkList.isEmpty()) {
			return BotMessage.simpleTextMessage("学废了！"+resp);
		} else {
			return BotMessage.simpleTextMessage("覆盖了！原本是"+respList);
		}
	}
}
