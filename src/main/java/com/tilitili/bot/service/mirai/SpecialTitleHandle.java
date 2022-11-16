package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotFriend;
import com.tilitili.common.entity.view.bot.BotGroup;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpecialTitleHandle extends ExceptionRespMessageToSenderHandle {
	private final BotManager botManager;

	@Autowired
	public SpecialTitleHandle(BotManager botManager) {
		this.botManager = botManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotEmum bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		String specialTitle = messageAction.getValue();
		List<Long> atList = messageAction.getAtList();
		Asserts.notBlank(specialTitle, "格式错啦(头衔)");
		Asserts.checkEquals(botSender.getSendType(), SendTypeEmum.GROUP_MESSAGE_STR, "啊嘞，不对劲");

		Long group = botSender.getGroup();
		Long qq = botUser.getQq();
		if (CollectionUtils.isNotEmpty(atList)) {
			qq = atList.get(0);
		}
		botManager.changeMemberInfo(bot, new BotFriend().setGroup(new BotGroup().setGroup(group)).setQq(qq).setSpecialTitle(specialTitle));
		return BotMessage.simpleTextMessage("搞定√");
	}
}
