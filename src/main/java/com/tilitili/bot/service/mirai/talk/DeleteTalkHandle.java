package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
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
		BotUser botUser = messageAction.getBotUser();
		String value = messageAction.getValueOrVirtualValue();
		String req = messageAction.getBodyOrDefault("提问", value);
		Long qqOrTinyId = botUser.getExternalId();

		String senderStatusKey = statusKey + qqOrTinyId;

		List<String> imageList = messageAction.getImageList();
		BotTalk botTalk = null;
		if (session.containsKey(senderStatusKey)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(TalkHandle.convertMessageToString(botMessage), botMessage);
		} else if (StringUtils.isNotBlank(req)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(Gsons.toJson(BotMessage.simpleTextMessage(req)), botMessage);
		} else if (CollectionUtils.isNotEmpty(imageList)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(Gsons.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(0)))), botMessage);
		} else if (! session.containsKey(senderStatusKey)) {
			session.put(senderStatusKey, "1");
			return BotMessage.simpleTextMessage("请告诉我关键词吧");
		}
//		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, botMessage);
		session.remove(senderStatusKey);
		Asserts.notNull(botTalk, "没找到。");
		botTalkMapper.updateBotTalkSelective(new BotTalk().setId(botTalk.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("移除了。");
	}


	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		BotUser botUser = botMessageAction.getBotUser();
		Long qqOrTinyId = botUser.getExternalId();
		BotSessionService.MiraiSession session = botMessageAction.getSession();
		String senderStatusKey = statusKey + qqOrTinyId;
		if (session.containsKey(senderStatusKey)) {
			return "移除对话";
		}
		return null;
	}

}
