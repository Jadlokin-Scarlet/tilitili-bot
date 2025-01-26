package com.tilitili.bot.service.mirai.event;

import com.google.common.reflect.TypeToken;
import com.tilitili.bot.component.JoinOrLeaveGameStatistics;
import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.component.CloseableRedisLock;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JoinOrLeaveGameEventHandle extends BaseEventHandleAdapt {
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final RedisCache redisCache;
	private final BotSenderTaskMappingManager botSenderTaskMappingManager;

	private Map<Long, List<String>> noticeMap;
	private final JoinOrLeaveGameStatistics joinOrLeaveGameStatistics;

	@Value("${JoinGameEventHandle.noticeMap:{}}")
	public void setNoticeMap(String noticeMapStr) {
		this.noticeMap = Gsons.fromJson(noticeMapStr, new TypeToken<Map<Long, List<String>>>(){}.getType());
	}



	@Autowired
	public JoinOrLeaveGameEventHandle(BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, RedisCache redisCache, @Qualifier("joinOrLeaveGameStatistics") JoinOrLeaveGameStatistics joinOrLeaveGameStatistics) {
		super(BotEvent.EVENT_TYPE_JOIN_OR_LEAVE_GAME);
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.redisCache = redisCache;
		this.joinOrLeaveGameStatistics = joinOrLeaveGameStatistics;
	}


	@Override
	public List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();

		List<BotMessage> respList = new ArrayList<>();
		try (CloseableRedisLock ignored = new CloseableRedisLock(redisCache, String.format("JoinOrLeaveGameEventHandle-lock-%s-%s", botSender.getId(), botUser.getId()))) {
			String listKey = String.format("JoinOrLeaveGameEventHandle-eventList-%s-%s", botSender.getId(), botUser.getId());
			String timeKey = String.format("JoinOrLeaveGameEventHandle-eventListTime-%s-%s", botSender.getId(), botUser.getId());
			Long time = redisCache.getValueLong(timeKey);
			if (time != null && System.currentTimeMillis() - time > 60 * 1000) {
				redisCache.delete(listKey);
				redisCache.delete(timeKey);
			}
			redisCache.rpush(listKey, botEvent.getMessage());
			redisCache.setValue(timeKey, System.currentTimeMillis());
		}
		joinOrLeaveGameStatistics.waitAndStat(botSender, botUser);

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