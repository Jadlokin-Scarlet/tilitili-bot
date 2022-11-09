package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupAdminHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;

	@Autowired
	public GroupAdminHandle(BotManager botManager) {
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKey();
		switch (key) {
			case "管理员": return handleAdmin(messageAction);
			case "取消管理员": return handleDeleteAdmin(messageAction);
		}
		return null;
	}

	private BotMessage handleAdmin(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		List<Long> atList = messageAction.getAtList();
		Long group = messageAction.getBotSender().getGroup();
		Asserts.notEmpty(atList, "谁?");
		for (Long qq : atList) {
			botManager.setMemberAdmin(bot, group, qq, true);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}

	private BotMessage handleDeleteAdmin(BotMessageAction messageAction) {
		BotEmum bot = messageAction.getBot();
		List<Long> atList = messageAction.getAtList();
		Long group = messageAction.getBotSender().getGroup();
		Asserts.notEmpty(atList, "谁?");
		for (Long qq : atList) {
			botManager.setMemberAdmin(bot, group, qq, false);
		}
		return BotMessage.simpleTextMessage("好了喵。");
	}
}
