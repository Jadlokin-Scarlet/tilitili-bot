package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Slf4j
//@Component
@Deprecated
public class QQGroupQueryHandle extends ExceptionRespMessageHandle {
	private final BotUserManager botUserManager;
	private final SendMessageManager sendMessageManager;
	private final BotRobotMapper botRobotMapper;

	public QQGroupQueryHandle(BotUserManager botUserManager, @Qualifier("sendMessageManager") SendMessageManager sendMessageManager, @Qualifier("botRobotMapper") BotRobotMapper botRobotMapper) {
		this.botUserManager = botUserManager;
		this.sendMessageManager = sendMessageManager;
		this.botRobotMapper = botRobotMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotSender botSender = messageAction.getBotSender();
		String name = messageAction.getValue();

		BotRobot guildBot = botRobotMapper.getValidBotRobotBySenderIdAndType(botSender.getId(), BotRobotConstant.TYPE_QQ_GUILD);
		Asserts.notNull(guildBot, "无法查询");

		List<BotUserDTO> userList = botUserManager.getValidBotUserBySenderIdAndNameWithParent(botSender.getId(), name);
		Asserts.checkEquals(userList.size(), 1);

		return BotMessage.simpleAtTextMessage("测试", userList.get(0))
				.setBotSender(botSender).setBot(guildBot);
	}
}
