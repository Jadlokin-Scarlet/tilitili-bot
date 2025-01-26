package com.tilitili.bot.component;

import com.google.common.collect.ImmutableMap;
import com.tilitili.common.component.CloseableRedisLock;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.CollectionUtils;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JoinOrLeaveGameStatistics {
	private final SendMessageManager sendMessageManager;
	private final RedisCache redisCache;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;
	private final BotSenderCacheManager botSenderCacheManager;

	public JoinOrLeaveGameStatistics(SendMessageManager sendMessageManager, RedisCache redisCache, @Qualifier("botForwardConfigMapper") BotForwardConfigMapper botForwardConfigMapper, @Qualifier("botSenderTaskMappingManager") BotSenderTaskMappingManager botSenderTaskMappingManager, @Qualifier("botSenderCacheManager") BotSenderCacheManager botSenderCacheManager) {
		this.sendMessageManager = sendMessageManager;
		this.redisCache = redisCache;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.botSenderCacheManager = botSenderCacheManager;
	}
	private final Map<String, String> textMap = ImmutableMap.of(
			BotEvent.JOIN_GAME_EVENT_MESSAGE, "加入",
			BotEvent.LEAVE_GAME_EVENT_MESSAGE, "退出"
	);

	@Async
	public void waitAndStat(BotSender botSender, BotUserDTO botUser) {
		TimeUtil.millisecondsSleep(4000);
		List<String> eventList;
		try (CloseableRedisLock ignored = new CloseableRedisLock(redisCache, String.format("JoinOrLeaveGameEventHandle-lock-%s-%s", botSender.getId(), botUser.getId()))) {
			String listKey = String.format("JoinOrLeaveGameEventHandle-eventList-%s-%s", botSender.getId(), botUser.getId());
			String timeKey = String.format("JoinOrLeaveGameEventHandle-eventListTime-%s-%s", botSender.getId(), botUser.getId());
			Long time = redisCache.getValueLong(timeKey);
			if (time == null || System.currentTimeMillis() - time < 3000) {
				log.info("进出统计被挤了！");
				return;
			}
			eventList = redisCache.lrange(listKey, 0, -1);
			redisCache.delete(listKey);
			redisCache.delete(timeKey);
		}
		if (CollectionUtils.isNotEmpty(eventList)) {
			BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
			List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

			if (!forwardConfigList.isEmpty()) {
				Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");
				for (BotForwardConfig forwardConfig : forwardConfigList) {
					try {
						Long targetSenderId = forwardConfig.getTargetSenderId();
						BotSender targetSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
						Asserts.notNull(targetSender, "找不到渠道");
						Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(targetSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");

						String text = eventList.stream().map(textMap::get).collect(Collectors.joining());
						String message = String.format("%s%s了游戏", botUser.getName(), text);

						sendMessageManager.sendMessage(BotMessage.simpleTextMessage(message).setBotSender(targetSender));
					} catch (AssertException e) {
						log.warn("转发渠道配置失败", e);
					}
				}
			}
		}
	}
}
