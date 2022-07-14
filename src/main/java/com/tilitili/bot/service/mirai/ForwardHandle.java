package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.emnus.MinecraftServerEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.MinecraftManager;
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
	private final MinecraftManager minecraftManager;

	public ForwardHandle(BotSenderMapper botSenderMapper, MinecraftManager minecraftManager) {
		this.baGroup = botSenderMapper.getBotSenderById(3759L);
		this.minecraftManager = minecraftManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();

		if (senderId == 4370L) {
			List<BotMessageChain> botMessageChainList = messageAction.getBotMessage().getBotMessageChainList().stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_IMAGE)).collect(Collectors.toList());
			if (!botMessageChainList.isEmpty() && blackList4370.contains(botMessageChainList.get(0).getUrl())) {
				return null;
			}
			return BotMessage.simpleListMessage(botMessageChainList).setSender(baGroup);
		}
		if (senderId == 4351L) {
			List<BotMessageChain> botMessageChainList = messageAction.getBotMessage().getBotMessageChainList().stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_SOURCE).negate()).collect(Collectors.toList());
			return BotMessage.simpleListMessage(botMessageChainList).setSender(baGroup);
		}
		if (senderId == 4407L) {
			String message = messageAction.getBotMessage().getBotMessageChainList().stream()
					.filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_PLAIN)).map(BotMessageChain::getText).collect(Collectors.joining());
			String sender = messageAction.getBotMessage().getGroupNickName();
			minecraftManager.sendMessage(MinecraftServerEmum.NIJISANJI_CHANNEL_MINECRAFT, String.format("%s：%s", sender, message));
			return BotMessage.emptyMessage();
		}
		return null;
	}
}
