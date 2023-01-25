package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupAdminHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final BotUserManager botUserManager;

	@Autowired
	public GroupAdminHandle(BotManager botManager, BotUserManager botUserManager) {
		this.botManager = botManager;
		this.botUserManager = botUserManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "管理员": return handleAdmin(messageAction);
			case "取消管理员": return handleDeleteAdmin(messageAction);
		}
		return null;
	}

	private BotMessage handleAdmin(BotMessageAction messageAction) {
		BotEnum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		List<Long> atList = messageAction.getAtList();
		Asserts.notEmpty(atList, "谁?");
		for (Long userId : atList) {
			BotUserDTO botUser = botUserManager.getBotUserByIdWithParent(userId);
			botManager.setMemberAdmin(bot, botSender, botUser, true);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}

	private BotMessage handleDeleteAdmin(BotMessageAction messageAction) {
		BotEnum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		List<Long> atList = messageAction.getAtList();
		Asserts.notEmpty(atList, "谁?");
		for (Long userId : atList) {
			BotUserDTO botUser = botUserManager.getBotUserByIdWithParent(userId);
			botManager.setMemberAdmin(bot, botSender, botUser, false);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}
}
