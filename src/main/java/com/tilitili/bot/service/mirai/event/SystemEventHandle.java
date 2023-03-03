package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SystemEventHandle extends BaseEventHandleAdapt {
	private final BotSenderMapper botSenderMapper;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public SystemEventHandle(BotSenderMapper botSenderMapper, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager) {
		super(BotEvent.EVENT_TYPE_SYSTEM);
		this.botSenderMapper = botSenderMapper;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
	}

	@Override
	public BotMessage handleEvent(BotEnum bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		Asserts.isFalse(BotUserConstant.BOT_USER_ID_LIST.contains(botUser.getId()), "系统消息屏蔽bot");

		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();
		Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");

		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetSender = botSenderMapper.getValidBotSenderById(targetSenderId);
			Asserts.notNull(targetSender, "找不到渠道");

			return BotMessage.simpleTextMessage(botEvent.getMessage()).setBotSender(targetSender);
		}
		return null;
	}
}