package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.constant.BotSenderConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.MinecraftServerEmum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.baidu.TranslateView;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BaiduManager;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.manager.SendMessageManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForwardHandle extends BaseMessageHandleAdapt {
	public static final String forwardMappingKey = "ForwardHandle.forwardMapping-";
	private final List<String> blackList4370 = Arrays.asList("https://gchat.qpic.cn/qmeetpic/49134681639135681/7091721-2888756874-C116108D71E375A0A37E168B6C97889A/0?term");
	private final MinecraftManager minecraftManager;
	private final BaiduManager baiduManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
	private final SendMessageManager sendMessageManager;
	private final RedisCache redisCache;

	private ScheduledFuture<?> future;

	public ForwardHandle(MinecraftManager minecraftManager, BaiduManager baiduManager, BotForwardConfigMapper botForwardConfigMapper, SendMessageManager sendMessageManager, RedisCache redisCache) {
		this.minecraftManager = minecraftManager;
		this.baiduManager = baiduManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.sendMessageManager = sendMessageManager;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();

		List<BotMessageChain> sourceMessageChainList = messageAction.getBotMessage().getBotMessageChainList();
		if (senderId == 4370L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).collect(Collectors.toList());
			if (!botMessageChainList.isEmpty() && blackList4370.contains(botMessageChainList.get(0).getUrl())) {
				return null;
			}
			return BotMessage.simpleListMessage(botMessageChainList).setSenderId(4454L);
		}
		if (senderId == 4351L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_SOURCE).negate()).collect(Collectors.toList());
			return BotMessage.simpleListMessage(botMessageChainList).setSenderId(3759L);
		}
		if (senderId == 3746L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_SOURCE).negate()).collect(Collectors.toList());
			return BotMessage.simpleListMessage(botMessageChainList).setSenderId(3384L);
		}

		for (MinecraftServerEmum server : MinecraftServerEmum.values()) {
			if (Objects.equals(senderId, server.getSenderId())) {
				String message = sourceMessageChainList.stream().map(chain -> {
					switch (chain.getType()) {
						case BotMessage.MESSAGE_TYPE_PLAIN: return chain.getText();
						case BotMessage.MESSAGE_TYPE_IMAGE:
							TranslateView translateView = baiduManager.translateImageIgnoreError(chain.getUrl());
							String imageText = translateView != null? translateView.getSumSrc(): null;
							return String.format("[图片%s]", StringUtils.isBlank(imageText)? "": ":"+ imageText);
						default: return "";
					}
				}).collect(Collectors.joining());
				String sender = messageAction.getBotUser().getName();
				minecraftManager.sendMessage(server, String.format("[%s]%s：%s", server.getPrefix(), sender, message));
				return BotMessage.emptyMessage();
			}
		}
		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(senderId).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);
		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			String sourceNameStr = forwardConfig.getSourceName() != null? forwardConfig.getSourceName() + "-": "";
			String userName = messageAction.getBotUser().getName();

			List<BotMessageChain> newMessageChainList = new ArrayList<>();
			newMessageChainList.add(BotMessageChain.ofPlain(String.format("[%s%s]%s：", sourceNameStr, messageAction.getBotSender().getName(), userName)));
			newMessageChainList.addAll(sourceMessageChainList);
			return BotMessage.simpleListMessage(newMessageChainList).setSenderId(targetSenderId);
		}

		if (BotSenderConstant.MIRAI_SENDER_ID.equals(senderId) && messageAction.getText() != null) {
			String text = messageAction.getText();
			List<String> resultList = StringUtils.extractList("(.+) 和 (.+) 开始比划牛子，", text);
			log.info("pkList = " + resultList);
			if (resultList.size() == 2 && (resultList.contains("Debris") || resultList.contains("Jadlokin_Scarlet"))) {
				if (future != null) {
					future.cancel(false);
				}
				future = scheduled.schedule(() -> {
					sendMessageManager.sendMessage(BotMessage.simpleListMessage(Arrays.asList(
							BotMessageChain.ofPlain("好啦。"),
							BotMessageChain.ofAt(BotUserConstant.MASTER_USER_ID)
					)).setBotSender(messageAction.getBotSender()));
				}, 5, TimeUnit.MINUTES);
			}
		}
		return null;
	}
}
