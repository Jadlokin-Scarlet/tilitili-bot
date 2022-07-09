package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.StreamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForwardHandle extends BaseMessageHandleAdapt {
	private final BotSender baGroup;
	private final List<String> blackList4370 = Arrays.asList("https://gchat.qpic.cn/qmeetpic/49134681639135681/7091721-2888756874-C116108D71E375A0A37E168B6C97889A/0?term");

	public ForwardHandle(BotSenderMapper botSenderMapper) {
		this.baGroup = botSenderMapper.getBotSenderById(3759L);
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		if (messageAction.getBotSender().getId() == 4370L) {
			List<BotMessageChain> botMessageChainList = messageAction.getBotMessage().getBotMessageChainList().stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).collect(Collectors.toList());
			if (!botMessageChainList.isEmpty() && blackList4370.contains(botMessageChainList.get(0).getUrl())) {
				return null;
			}
			return BotMessage.simpleListMessage(botMessageChainList).setSender(baGroup);
		}
		return null;
	}
}
