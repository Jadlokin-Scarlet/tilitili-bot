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

@Component
public class DeleteTalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;

	@Autowired
	public DeleteTalkHandle(BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
		this.botTalkMapper = botTalkMapper;
		this.botTalkManager = botTalkManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String req = messageAction.getBodyOrDefault("提问", messageAction.getValue());
		List<String> imageList = messageAction.getImageList();
		if (StringUtils.isBlank(req)) {
			Asserts.notEmpty(imageList, "格式错啦(提问)");
			req = QQUtil.getImageUrl(imageList.get(0));
		}

		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, messageAction.getBotMessage());
		Asserts.notEquals(botTalkList.size(), 0, "没找到。");

		BotTalk botTalk = botTalkList.get(0);
		botTalkMapper.updateBotTalkSelective(new BotTalk().setId(botTalk.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("移除了。");
	}
}
