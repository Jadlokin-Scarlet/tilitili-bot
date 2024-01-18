package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.HongResult;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.openai.ChoiceMessage;
import com.tilitili.common.manager.HongManager;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class HongHandle extends ExceptionRespMessageHandle {
	private final RedisCache redisCache;
	private final HongManager hongManager;

	public HongHandle(RedisCache redisCache, HongManager hongManager) {
		this.redisCache = redisCache;
		this.hongManager = hongManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "哄哄": return this.handleStart(messageAction);
			case "不哄了": return this.handleEnd(messageAction);
			default: return this.handleChat(messageAction);
		}
	}

	private BotMessage handleEnd(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		if (redisCache.exists(HongManager.chatKey + botSender.getId())) {
			return BotMessage.simpleTextMessage("当前还没有在哄哦。");
		}
		redisCache.delete(HongManager.chatKey + botSender.getId());
		redisCache.delete(HongManager.scoreKey + botSender.getId());
		redisCache.delete(HongManager.numKey + botSender.getId());
		return BotMessage.simpleTextMessage("(╯‵□′)╯︵┻━┻");
	}

	private BotMessage handleChat(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		if (!redisCache.exists(HongManager.chatKey + botSender.getId())) {
			return null;
		}
		HongResult result = hongManager.chat(botSender, messageAction.getText());
		Long score = redisCache.increment(HongManager.scoreKey + botSender.getId(), result.getScore());
		Long num = redisCache.increment(HongManager.numKey + botSender.getId(), 1);
		return BotMessage.simpleListMessage(Arrays.asList(
				BotMessageChain.ofPlain(result.getMessage()),
				BotMessageChain.ofPlain(String.format("\n(原谅值:%s)(轮次:%s/10)", score, num))
		));
	}

	private BotMessage handleStart(BotMessageAction messageAction) {
		BotSender botSender = messageAction.getBotSender();
		if (redisCache.exists(HongManager.chatKey + botSender.getId())) {
			return BotMessage.simpleTextMessage("已经在哄啦，别急，或者发送[不哄了]。");
		}
		redisCache.delete(HongManager.scoreKey + botSender.getId());
		redisCache.delete(HongManager.numKey + botSender.getId());

		List<ChoiceMessage> chatList = new ArrayList<>();
		chatList.add(new ChoiceMessage().setRole(ChoiceMessage.aiRole).setContent("哼"));
		redisCache.setValue(HongManager.chatKey + botSender.getId(), Gsons.toJson(chatList), 60 * 60);
		redisCache.increment(HongManager.scoreKey + botSender.getId(), 20L);
		redisCache.increment(HongManager.numKey + botSender.getId(), 0L);
		return BotMessage.simpleTextMessage("你回家太晚，女朋友很生气，试着安慰她吧。");
	}
}
