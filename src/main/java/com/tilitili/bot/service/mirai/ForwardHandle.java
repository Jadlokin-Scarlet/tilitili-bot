package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
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

	public ForwardHandle(BotForwardConfigMapper botForwardConfigMapper) {
		this.botForwardConfigMapper = botForwardConfigMapper;
	}

	@Override
	public List<BotMessage> handleMessageNew(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		Long senderId = messageAction.getBotSender().getId();

		List<BotMessageChain> sourceMessageChainList = messageAction.getBotMessage().getBotMessageChainList();
		if (senderId == 4518L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).collect(Collectors.toList());
			if (!botMessageChainList.isEmpty() && blackList4370.contains(botMessageChainList.get(0).getUrl())) {
				return null;
			}
			return Arrays.asList(
					BotMessage.simpleListMessage(botMessageChainList).setSenderId(4454L),
					BotMessage.simpleListMessage(botMessageChainList).setSenderId(4528L)
			);
		}
		if (senderId == 4351L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_SOURCE).negate()).collect(Collectors.toList());
			return Collections.singletonList(BotMessage.simpleListMessage(botMessageChainList).setSenderId(3759L));
		}
		if (senderId == 3746L) {
			List<BotMessageChain> botMessageChainList = sourceMessageChainList.stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_SOURCE).negate()).collect(Collectors.toList());
			return Collections.singletonList(BotMessage.simpleListMessage(botMessageChainList).setSenderId(3384L));
		}

//		for (MinecraftServerEnum server : MinecraftServerEnum.values()) {
//			if (Objects.equals(senderId, server.getSenderId())) {
//				String message = sourceMessageChainList.stream().map(chain -> {
//					switch (chain.getType()) {
//						case BotMessage.MESSAGE_TYPE_PLAIN: return chain.getText();
//						case BotMessage.MESSAGE_TYPE_IMAGE:
//							TranslateView translateView = baiduManager.translateImageIgnoreError(chain.getUrl());
//							String imageText = translateView != null? translateView.getSumSrc(): null;
//							return String.format("[图片%s]", StringUtils.isBlank(imageText)? "": ":"+ imageText);
//						default: return "";
//					}
//				}).collect(Collectors.joining());
//				String sender = messageAction.getBotUser().getName();
//				minecraftManager.sendMessage(server, String.format("[%s]%s：%s", server.getPrefix(), sender, message));
//				return BotMessage.emptyMessage();
//			}
//		}
		BotForwardConfigQuery forwardConfigQuery = new BotForwardConfigQuery().setSourceSenderId(senderId).setStatus(0).setIsSend(true);
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(forwardConfigQuery);
		List<BotMessage> respMessageList = new ArrayList<>();
		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			String sourceNameStr = StringUtils.isNotBlank(forwardConfig.getSourceName())? "["+forwardConfig.getSourceName()+"]": "";
			String userName = messageAction.getBotUser().getName();

			List<BotMessageChain> newMessageChainList = new ArrayList<>();
			newMessageChainList.add(BotMessageChain.ofSpeaker(userName, sourceNameStr));
			newMessageChainList.addAll(sourceMessageChainList.stream().map(chain -> {
				switch (chain.getType()) {
					case BotMessage.MESSAGE_TYPE_SOURCE: return null;
					case BotMessage.MESSAGE_TYPE_AT: return Objects.equals(chain.getTarget().getId(), bot.getUserId()) ? null: chain;
					default: return chain;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList()));
			respMessageList.add(BotMessage.simpleListMessage(newMessageChainList).setSenderId(targetSenderId));
		}
		return respMessageList;

//		if (BotSenderConstant.MIRAI_SENDER_ID.equals(senderId) && messageAction.getText() != null) {
//			String text = messageAction.getText();
//			List<String> resultList = StringUtils.extractList("(.+) 和 (.+) 开始比划牛子，", text);
//			log.info("pkList = " + resultList);
//			if (resultList.size() == 2 && (resultList.contains("Debris") || resultList.contains("Jadlokin_Scarlet"))) {
//				if (future != null) {
//					future.cancel(false);
//				}
//				future = scheduled.schedule(() -> {
//					sendMessageManager.sendMessage(BotMessage.simpleListMessage(Arrays.asList(
//							BotMessageChain.ofPlain("好啦。"),
//							BotMessageChain.ofAt(BotUserConstant.MASTER_USER_ID)
//					)).setBotSender(messageAction.getBotSender()));
//				}, 5, TimeUnit.MINUTES);
//			}
//		}
//		return null;
	}
}
