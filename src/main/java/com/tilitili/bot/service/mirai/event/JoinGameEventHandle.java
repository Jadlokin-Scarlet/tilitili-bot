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
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class JoinGameEventHandle extends BaseEventHandleAdapt {
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final SendMessageManager sendMessageManager;
	private final RedisCache redisCache;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	@Autowired
	public JoinGameEventHandle(BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, SendMessageManager sendMessageManager, RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_JOIN_GAME);
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.sendMessageManager = sendMessageManager;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleEvent(BotRobot bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();

		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

		if (!forwardConfigList.isEmpty()) {
			Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");
			for (BotForwardConfig forwardConfig : forwardConfigList) {
				Long targetSenderId = forwardConfig.getTargetSenderId();
				BotSender targetSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
				Asserts.notNull(targetSender, "找不到渠道");
				Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(targetSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");

				sendMessageManager.sendMessage(BotMessage.simpleTextMessage(botEvent.getMessage()).setBotSender(targetSender));
			}
		}

		String key = String.format("JoinGameEventHandle-%s", botUser.getId());
		if (redisCache.exists(key)) {
			return BotMessage.emptyMessage();
		}
		redisCache.setValue(key, "yes", 10);

		String time = TimeUtil.getTimeTalk();
		return BotMessage.simpleTextMessage(String.format("%s好，%s，今天也是充满希望的一天。", time, botUser.getName()));
	}
}