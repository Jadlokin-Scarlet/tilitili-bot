package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotRobot;
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
public class MuteHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final BotUserManager botUserManager;

	@Autowired
	public MuteHandle(BotManager botManager, BotUserManager botUserManager) {
		this.botManager = botManager;
		this.botUserManager = botUserManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotRobot bot = messageAction.getBot();
		String key = messageAction.getKeyWithoutPrefix();
		List<BotUserDTO> atList = messageAction.getAtList();
		BotSender botSender = messageAction.getBotSender();
		Asserts.notEmpty(atList, "格式不对喵(at)");
		BotUserDTO atUser = atList.get(0);
		Long atUserId = atUser.getId();

		if ("禁言".equals(key)) {
//			String timeStr = messageAction.getValueOrDefault("60");
//			Asserts.isNumber(timeStr, "格式不对喵(秒数)");
//			int time = Integer.parseInt(timeStr);
//			Asserts.isTrue(time <= 60, "最多一分钟喵");
			if (!BotUserConstant.MASTER_USER_ID.equals(messageAction.getBotUser().getId())) {
				Asserts.notEquals(atUserId, BotUserConstant.MASTER_USER_ID, "不准喵");
			}

			botManager.muteMember(bot, botSender, atUser, 60);
		} else if ("解除禁言".equals(key)) {
			botManager.unMuteMember(bot, botSender, atUser);
		}

		return BotMessage.simpleTextMessage("好了喵");
	}
}
