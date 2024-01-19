package com.tilitili.bot.service.mirai.event;

import com.google.common.reflect.TypeToken;
import com.tilitili.bot.service.mirai.base.BaseEventHandleAdapt;
import com.tilitili.common.constant.BotTaskConstant;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotEvent;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotSenderTaskMappingManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JoinGameEventHandle extends BaseEventHandleAdapt {
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
	public JoinGameEventHandle(BotSenderCacheManager botSenderCacheManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderTaskMappingManager botSenderTaskMappingManager, RedisCache redisCache) {
		super(BotEvent.EVENT_TYPE_JOIN_GAME);
		this.botSenderCacheManager = botSenderCacheManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderTaskMappingManager = botSenderTaskMappingManager;
		this.redisCache = redisCache;
	}

	@Override
	public List<BotMessage> handleEventNew(BotRobot bot, BotMessage botMessage) {
		BotUserDTO botUser = botMessage.getBotUser();
		BotSender botSender = botMessage.getBotSender();
		BotEvent botEvent = botMessage.getBotEvent();

		List<BotMessage> respList = new ArrayList<>();

		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(botSender.getId()).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);

		if (!forwardConfigList.isEmpty()) {
			Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(botSender.getId(), BotTaskConstant.ForwardTaskId), "无转发权限");
			for (BotForwardConfig forwardConfig : forwardConfigList) {
				Long targetSenderId = forwardConfig.getTargetSenderId();
				BotSender targetSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
				Asserts.notNull(targetSender, "找不到渠道");
				Asserts.isTrue(botSenderTaskMappingManager.checkSenderHasTask(targetSender.getId(), BotTaskConstant.helpTaskId), "无帮助权限");

				respList.add(BotMessage.simpleTextMessage(botEvent.getMessage()).setBotSender(targetSender));
			}
		}

		String key = String.format("JoinGameEventHandle-%s", botUser.getId());
		if (!redisCache.exists(key)) {
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