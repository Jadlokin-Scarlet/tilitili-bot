package com.tilitili.bot.service;

import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotFriend;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.QQUtil;
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

	@Autowired
	public FunctionTalkService(BotManager botManager) {
		this.botManager = botManager;
	}

	public static String convertMessageToString(BotMessage botMessage) {
		return Gsons.toJson(BotMessage.simpleListMessage(botMessage.getBotMessageChainList()));
	}

	public List<BotMessageChain> convertCqToMessageChain(String messageStr) {
		String[] textList = messageStr.split("\\[CQ:\\w+[^\\[\\]]*]");
		Matcher cqMatcher = Pattern.compile("\\[CQ:\\w+[^\\[\\]]*]").matcher(messageStr);

		List<BotMessageChain> botMessageChain = new ArrayList<>();

		for (String s : textList) {
			if (StringUtils.isNotBlank(s)) {
				botMessageChain.add(BotMessageChain.ofPlain(s));
			}
			if (cqMatcher.find()) {
				findNextCqToChain(cqMatcher, botMessageChain);
			}
		}

		while (cqMatcher.find()) {
			findNextCqToChain(cqMatcher, botMessageChain);
		}
		return botMessageChain;
	}

	private void findNextCqToChain(Matcher cqMatcher, List<BotMessageChain> botMessageChain) {
		String cq = cqMatcher.group();
		String messageType = StringUtils.patten1("CQ:(\\w+)", cq);
		Asserts.notBlank(messageType, "gocq正则解析异常, text=%s", cq);

		switch (messageType) {
			case "image": botMessageChain.add(BotMessageChain.ofImage(StringUtils.patten1(",url=([^=,\\]]+)", cq)));break;
			case "at": {
				String qq = StringUtils.patten1(",qq=(\\d+|all)", cq);
				if (!Objects.equals(qq, "all")) {
					botMessageChain.add(BotMessageChain.ofAt(Long.valueOf(qq)));
				}
				break;
			}
			case "face": botMessageChain.add(BotMessageChain.ofFace(Integer.valueOf(StringUtils.patten1(",id=([0-9\\-]+)", cq))));
			case "memberName": botMessageChain.add(new BotMessageChain().setType("memberName").setTarget(Long.valueOf(StringUtils.patten1(",qq=([0-9\\-]+)", cq))));
			case "portrait": botMessageChain.add(new BotMessageChain().setType("portrait").setTarget(Long.valueOf(StringUtils.patten1(",qq=([0-9\\-]+)", cq))));
		}
	}

	public void supplementChain(BotEmum bot, BotSender botSender, BotMessage respMessage) {
		List<BotMessageChain> botMessageChainList = respMessage.getBotMessageChainList();
		for (BotMessageChain botMessageChain : botMessageChainList) {
			switch (botMessageChain.getType()) {
				case "memberName": {
					BotFriend botFriend = botManager.getMemberInfo(bot, botSender.getGroup(), botMessageChain.getTarget());
					Asserts.notNull(botFriend, "啊嘞，不对劲");
					Asserts.notNull(botFriend.getMemberName(), "啊嘞，不对劲");
					botMessageChain.setType(BotMessage.MESSAGE_TYPE_PLAIN);
					botMessageChain.setText(botFriend.getMemberName());
					break;
				}
				case "portrait": {
					botMessageChain.setType(BotMessage.MESSAGE_TYPE_IMAGE);
					botMessageChain.setUrl(QQUtil.getFriendPortrait(botMessageChain.getTarget()));
				}
			}
		}
	}
}
