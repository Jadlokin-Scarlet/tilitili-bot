package com.tilitili.bot.service;

import com.tilitili.common.emnus.BotEnum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FunctionTalkService {
	private final BotManager botManager;
	private final BotUserManager botUserManager;

	@Autowired
	public FunctionTalkService(BotManager botManager, BotUserManager botUserManager) {
		this.botManager = botManager;
		this.botUserManager = botUserManager;
	}

	public static String convertMessageToString(BotMessage botMessage) {
		return Gsons.toJson(BotMessage.simpleListMessage(botMessage.getBotMessageChainList()));
	}

	public List<BotMessageChain> convertCqToMessageChain(BotSender botSender, String messageStr) {
		String[] textList = messageStr.split("\\[CQ:\\w+[^\\[\\]]*]");
		Matcher cqMatcher = Pattern.compile("\\[CQ:\\w+[^\\[\\]]*]").matcher(messageStr);

		List<BotMessageChain> botMessageChain = new ArrayList<>();

		for (String s : textList) {
			if (StringUtils.isNotBlank(s)) {
				botMessageChain.add(BotMessageChain.ofPlain(s));
			}
			if (cqMatcher.find()) {
				findNextCqToChain(botSender, cqMatcher, botMessageChain);
			}
		}

		while (cqMatcher.find()) {
			findNextCqToChain(botSender, cqMatcher, botMessageChain);
		}
		return botMessageChain;
	}

	private void findNextCqToChain(BotSender botSender, Matcher cqMatcher, List<BotMessageChain> botMessageChain) {
		BotEnum bot = BotEnum.getBotById(botSender.getBot());
		String cq = cqMatcher.group();
		String messageType = StringUtils.patten1("CQ:(\\w+)", cq);
		Asserts.notBlank(messageType, "gocq正则解析异常, text=%s", cq);

		switch (messageType) {
			case "image": botMessageChain.add(BotMessageChain.ofImage(StringUtils.patten1(",url=([^=,\\]]+)", cq)));break;
			case "at": {
				String externalId = StringUtils.patten1(",qq=(\\d+|all)", cq);
				if (!Objects.equals(externalId, "all")) {
					BotUserDTO botUser = botManager.addOrUpdateBotUser(bot, botSender, Long.valueOf(externalId));
					botMessageChain.add(BotMessageChain.ofAt(botUser));
				}
				break;
			}
			case "face": botMessageChain.add(BotMessageChain.ofFace(Integer.valueOf(StringUtils.patten1(",id=([0-9\\-]+)", cq))));break;
			case "memberName": {
				Long externalId = Long.valueOf(StringUtils.patten1(",qq=([0-9\\-]+)", cq));
				BotUserDTO botUser = botManager.addOrUpdateBotUser(bot, botSender, externalId);
				botMessageChain.add(new BotMessageChain().setType("memberName").setTarget(botUser));
				break;
			}
			case "portrait": {
				Long externalId = Long.valueOf(StringUtils.patten1(",qq=([0-9\\-]+)", cq));
				BotUserDTO botUser = botManager.addOrUpdateBotUser(bot, botSender, externalId);
				botMessageChain.add(new BotMessageChain().setType("portrait").setTarget(botUser));
				break;
			}
			case "enter": botMessageChain.add(BotMessageChain.ofPlain("\n"));
		}
	}

	public void supplementChain(BotEnum bot, BotSender botSender, BotMessage respMessage) {
		List<BotMessageChain> botMessageChainList = respMessage.getBotMessageChainList();
		for (BotMessageChain botMessageChain : botMessageChainList) {
			switch (botMessageChain.getType()) {
				case "memberName": {
					BotUserDTO botUser = botMessageChain.getTarget();
					Asserts.notNull(botUser, "啊嘞，不对劲");
					Asserts.notNull(botUser.getName(), "查无此人");
					botMessageChain.setType(BotMessage.MESSAGE_TYPE_PLAIN);
					botMessageChain.setText(botUser.getName());
					break;
				}
				case "portrait": {
					BotUserDTO botUser = botMessageChain.getTarget();
					Asserts.notNull(botUser, "啊嘞，不对劲");
					Asserts.notNull(botUser.getFace(), "查无此人");
					botMessageChain.setType(BotMessage.MESSAGE_TYPE_IMAGE);
					botMessageChain.setUrl(botUser.getFace());
				}
			}
		}
	}
}
