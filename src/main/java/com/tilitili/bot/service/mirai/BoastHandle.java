package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson2.JSONPath;
import com.google.common.collect.Lists;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotBoast;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotBoastQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.mapper.mysql.BotBoastMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class BoastHandle extends ExceptionRespMessageHandle {
	private final BotBoastMapper botBoastMapper;
	private final Random random = new Random(System.currentTimeMillis());

	@Autowired
	public BoastHandle(BotBoastMapper botBoastMapper) {
		this.botBoastMapper = botBoastMapper;
	}
	@Override
	protected List<BotMessage> mockMessageInWaiteSender(BotMessageAction messageAction) {
		String key = messageAction.getKeyWithoutPrefix();

		switch (key) {
			case "夸夸我": return Collections.singletonList(BotMessage.simpleTextMessage("我吃过的山珍海味，皆不及你珍贵，更不及你可口。"));
			default: return Collections.emptyList();
		}
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String key = messageAction.getKeyWithoutPrefix();
		List<BotUserDTO> atList = messageAction.getAtList();
		String text;
		String type;
		if (Arrays.asList("夸夸他", "kkt", "夸夸我", "kkw").contains(key)) {
			type = "夸夸他";
			String result = HttpClientUtil.httpGet("https://api.shadiao.app/chp");
			if (StringUtils.isNotBlank(result)) {
				text = (String) JSONPath.eval(result, "$.data.text");
			} else {
				text = null;
			}
		} else if (Arrays.asList("骂骂我", "骂骂他").contains(key)){
			text = "";//HttpClientUtil.httpGet("https://nmsl.yfchat.xyz/api.php?level=min");
			type = "骂骂他";
		} else {
			throw new AssertException();
		}
		if (StringUtils.isBlank(text) || text.contains("流量包")) {
			List<BotBoast> boastList = botBoastMapper.getBotBoastByCondition(new BotBoastQuery().setType(type));
			text = boastList.get(random.nextInt(boastList.size())).getText();
		} else {
			if (botBoastMapper.getBotBoastByCondition(new BotBoastQuery().setText(text).setType(type)).isEmpty()) {
				botBoastMapper.addBotBoastSelective(new BotBoast().setText(text).setType(type));
			}
		}
		Asserts.notBlank(text, "网络异常");

		boolean isOther = Arrays.asList("夸夸他", "kkt", "骂骂他").contains(key);
		if (isOther) {
			BotUserDTO firstAt = atList.stream().findFirst().orElse(null);
			Asserts.notNull(firstAt, "谁？");
			return BotMessage.simpleListMessage(Lists.newArrayList(
					BotMessageChain.ofAt(firstAt),
					BotMessageChain.ofPlain(" "),
					BotMessageChain.ofPlain(text)
			));
		} else {
			return BotMessage.simpleTextMessage(text);
		}
	}
}
