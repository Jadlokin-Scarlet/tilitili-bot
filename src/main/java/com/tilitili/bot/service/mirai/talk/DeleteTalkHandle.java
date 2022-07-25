package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteTalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;
	private static final String statusKey = "DeleteTalkHandle.statusKey";

	@Autowired
	public DeleteTalkHandle(BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
		this.botTalkMapper = botTalkMapper;
		this.botTalkManager = botTalkManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		String value = messageAction.getValue();
		String req = messageAction.getBodyOrDefault("提问", value);
		Long qqOrTinyId = messageAction.getQqOrTinyId();

		String senderStatusKey = statusKey + qqOrTinyId;

		List<String> imageList = messageAction.getImageList();
		BotTalk botTalk = null;
		if (session.containsKey(senderStatusKey)) {
			req = TalkHandle.convertMessageToString(botMessage);
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(req, botMessage);
		} else if (StringUtils.isNotBlank(req)) {
			List<BotTalk> talkList = botTalkManager.getBotTalkByBotMessage(req, 0, botMessage);
			Asserts.notEmpty(talkList, "没找到");
			botTalk = talkList.get(0);
		} else if (CollectionUtils.isNotEmpty(imageList)) {
			List<BotTalk> talkList = botTalkManager.getBotTalkByBotMessage(QQUtil.getImageUrl(imageList.get(0)), 1, botMessage);
			Asserts.notEmpty(talkList, "没找到");
			botTalk = talkList.get(0);
		} else if (! session.containsKey(senderStatusKey)) {
			session.put(senderStatusKey, "1");
			return BotMessage.simpleTextMessage("请告诉我关键词吧");
		}
//		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, botMessage);
		Asserts.notNull(botTalk, "没找到。");
		session.remove(senderStatusKey);
		botTalkMapper.updateBotTalkSelective(new BotTalk().setId(botTalk.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("移除了。");
	}
}
