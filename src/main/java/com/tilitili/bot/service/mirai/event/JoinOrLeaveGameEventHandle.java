package com.tilitili.bot.service.mirai.event;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.component.CloseableRedisLock;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JoinOrLeaveGameEventHandle extends BaseEventHandleAdapt {
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final RedisCache redisCache;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	private Map<Long, List<String>> noticeMap;

	@Value("${JoinGameEventHandle.noticeMap:{}}")
	public void setNoticeMap(String noticeMapStr) {
		this.noticeMap = Gsons.fromJson(noticeMapStr, new TypeToken<Map<Long, List<String>>>(){}.getType());
	}



	@Autowired
	public JoinOrLeaveGameEventHandle(BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_JOIN_OR_LEAVE_GAME);
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.redisCache = redisCache;
	}

	private final Map<String, String> textMap = ImmutableMap.of(
			BotEvent.JOIN_GAME_EVENT_MESSAGE, "加入",
			BotEvent.LEAVE_GAME_EVENT_MESSAGE, "退出"
	);

	@Override
	public List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();

		List<BotMessage> respList = new ArrayList<>();
		List<String> eventList = null;
		try (CloseableRedisLock ignored = new CloseableRedisLock(redisCache, String.format("JoinOrLeaveGameEventHandle-lock-%s-%s", botSender.getId(), botUser.getId()))) {
			String listKey = String.format("JoinOrLeaveGameEventHandle-eventList-%s-%s", botSender.getId(), botUser.getId());
			String timeKey = String.format("JoinOrLeaveGameEventHandle-eventListTime-%s-%s", botSender.getId(), botUser.getId());
			Long time = redisCache.getValueLong(timeKey);
			if (System.currentTimeMillis() - time < 3000) {
				redisCache.rpush(listKey, botEvent.getMessage());
				redisCache.setValue(timeKey, System.currentTimeMillis());
			} else {
				eventList = redisCache.lrange(listKey, 0, -1);
				redisCache.delete(listKey);
				redisCache.delete(timeKey);
			}
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

						respList.add(BotMessage.simpleTextMessage(message).setBotSender(targetSender));
					} catch (AssertException e) {
						log.warn("转发渠道配置失败", e);
					}
				}
			}
		}

		String key = String.format("JoinOrLeaveGameEventHandle-%s", botUser.getId());
		if (BotEvent.JOIN_GAME_EVENT_MESSAGE.equals(botEvent.getMessage()) && !redisCache.exists(key)) {
			redisCache.setValue(key, "yes", 10);

			String time = TimeUtil.getTimeTalk();
			respList.add(BotMessage.simpleTextMessage(String.format("%s好，%s，今天也是充满希望的一天。", time, botUser.getName())));

			if (noticeMap.containsKey(botSender.getId())) {
				List<String> noticeList = noticeMap.get(botSender.getId());
				Long num = redisCache.increment("JoinGame.niticeNum-" + botSender.getId());
				respList.add(BotMessage.simpleListMessage(Arrays.asList(
						BotMessageChain.ofSpeaker("公告"),
						BotMessageChain.ofPlain(noticeList.get((int) (num % noticeList.size())))
				)));
			}
		}

		if (respList.isEmpty()) {
			return null;
		}
		return respList;
	}
}