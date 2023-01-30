package com.tilitili.bot.service.mirai.event;

import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.List;

@Slf4j
@Component
public class JoinGameEventHandle extends BaseEventHandleAdapt {
	private final BotSenderMapper botSenderMapper;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final SendMessageManager sendMessageManager;
	private final RedisCache redisCache;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public JoinGameEventHandle(BotSenderMapper botSenderMapper, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, SendMessageManager sendMessageManager, RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_JOIN_GAME);
		this.botSenderMapper = botSenderMapper;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.sendMessageManager = sendMessageManager;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleEvent(BotEnum bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();
		Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");

		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetSender = botSenderMapper.getValidBotSenderById(targetSenderId);
			Asserts.notNull(targetSender, "找不到渠道");
			Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(targetSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");

			sendMessageManager.sendMessage(BotMessage.simpleTextMessage(botEvent.getMessage()).setBotSender(targetSender));
		}

		String key = String.format("JoinGameEventHandle-%s", botUser.getId());
		if (redisCache.exists(key)) {
			return BotMessage.emptyMessage();
		}
		redisCache.setValue(key, "yes", 10);

		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		String time = "早上";
		if (hour > 9) time = "中午";
		if (hour > 12) time = "下午";
		if (hour > 18) time = "晚上";
		return BotMessage.simpleTextMessage(String.format("%s，%s，今天也是充满希望的一天。", time, botUser));
	}
}