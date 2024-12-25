package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class SystemEventHandle extends BaseEventHandleAdapt {
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;
	private final BotRobotCacheManager botRobotCacheManager;

	@Autowired
	public SystemEventHandle(BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, BotRobotCacheManager botRobotCacheManager) {
		super(BotEvent.EVENT_TYPE_SYSTEM);
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.botRobotCacheManager = botRobotCacheManager;
	}

	private final List<Integer> messageTypeList = Arrays.asList(0, 2);

	@Override
	public List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		List<Long> botRobotUserIdList = botRobotCacheManager.getBotRobotUserIdList();
		Asserts.isFalse(botRobotUserIdList.contains(botUser.getId()), "系统消息屏蔽bot");

		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();
		Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");

		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

		List<BotMessage> respList = new ArrayList<>();
		for (BotForwardConfig forwardConfig : forwardConfigList) {
			try {
				if (messageTypeList.contains(forwardConfig.getMessageType())) {
					Long targetSenderId = forwardConfig.getTargetSenderId();
					BotSender targetSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
					Asserts.notNull(targetSender, "找不到渠道");

					BotMessage resp = BotMessage.simpleTextMessage(botEvent.getMessage()).setBotSender(targetSender);
					respList.add(resp);
				}
			} catch (AssertException e) {
				log.warn("转发渠道配置失败", e);
			}
		}
		return respList;
	}
}