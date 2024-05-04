package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForwardHandle extends BaseMessageHandleAdapt {
	private final List<String> blackList4370 = Arrays.asList("https://gchat.qpic.cn/qmeetpic/49134681639135681/7091721-2888756874-C116108D71E375A0A37E168B6C97889A/0?term");
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final RedisCache redisCache;
	private final BotSenderCacheManager botSenderCacheManager;

	public ForwardHandle(BotForwardConfigMapper botForwardConfigMapper, RedisCache redisCache, BotSenderCacheManager botSenderCacheManager) {
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.redisCache = redisCache;
		this.botSenderCacheManager = botSenderCacheManager;
	}

	@Override
	public List<BotMessage> handleMessageNew(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		Long senderId = messageAction.getBotSender().getId();

		List<BotMessageChain> sourceMessageChainList = messageAction.getBotMessage().getBotMessageChainList();
		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(senderId).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);
		List<BotMessage> respMessageList = new ArrayList<>();
		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
			Asserts.notNull(targetSender, "权限不足");

			String sourceNameStr = StringUtils.isNotBlank(forwardConfig.getSourceName())? "["+forwardConfig.getSourceName()+"]": "";
			String userName = messageAction.getBotUser().getName();

			List<BotMessageChain> newMessageChainList = new ArrayList<>();
			if (forwardConfig.getMessageType() == 0) {
				newMessageChainList.add(BotMessageChain.ofSpeaker(userName, sourceNameStr));
				newMessageChainList.addAll(sourceMessageChainList.stream().map(chain -> {
					switch (chain.getType()) {
						case BotMessage.MESSAGE_TYPE_SOURCE: return null;
						case BotMessage.MESSAGE_TYPE_AT: return Objects.equals(chain.getTarget().getId(), bot.getUserId()) ? null: chain;
						default: return chain;
					}
				}).filter(Objects::nonNull).collect(Collectors.toList()));

				// 往Minecraft的文字传话特别提醒
				String key = "Minecraft-message-reply-"+senderId;
				if (!redisCache.exists(key) && SendTypeEnum.MINECRAFT_MESSAGE_STR.equals(targetSender.getSendType())) {
					respMessageList.add(BotMessage.simpleTextMessage("连接已建立。", messageAction.getBotMessage()));
				}
				redisCache.setValue(key, "yes", 5 * 60);
			} else if (forwardConfig.getMessageType() == 1) {
				newMessageChainList = sourceMessageChainList.stream()
						.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).collect(Collectors.toList());
				if (!newMessageChainList.isEmpty() && blackList4370.contains(newMessageChainList.get(0).getUrl())) {
					return null;
				}
			} else if (forwardConfig.getMessageType() == 2) {
				return null;
			} else {
				throw new AssertException();
			}
			respMessageList.add(BotMessage.simpleListMessage(newMessageChainList).setSenderId(targetSenderId));
		}
		return respMessageList;
	}
}
