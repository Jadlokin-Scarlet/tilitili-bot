package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.CheckManager;
import com.tilitili.common.utils.Asserts;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpecialTitleHandle extends ExceptionRespMessageToSenderHandle {
	private final BotManager botManager;
	private final CheckManager checkManager;
	private final BotUserManager botUserManager;

	@Autowired
	public SpecialTitleHandle(BotManager botManager, CheckManager checkManager, BotUserManager botUserManager) {
		this.botManager = botManager;
		this.checkManager = checkManager;
		this.botUserManager = botUserManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotEnum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		String specialTitle = messageAction.getValue();
		List<Long> atList = messageAction.getAtList();
		Asserts.notBlank(specialTitle, "格式错啦(头衔)");
		Asserts.checkEquals(botSender.getSendType(), SendTypeEnum.GROUP_MESSAGE_STR, "啊嘞，不对劲");

		if (CollectionUtils.isNotEmpty(atList)) {
			botUser = botUserManager.getBotUserByIdWithParent(atList.get(0));
		}
		specialTitle = checkManager.checkAndReplaceTextCache(specialTitle);
		botManager.changeMemberInfo(bot, botSender, botUser, null, specialTitle);
		return BotMessage.simpleTextMessage("搞定√");
	}
}
