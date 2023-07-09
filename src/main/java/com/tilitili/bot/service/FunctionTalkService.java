package com.tilitili.bot.service;

import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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

	public List<BotMessageChain> convertCqToMessageChain(BotRobot bot, BotSender botSender, String messageStr) {
		String[] textList = messageStr.split("\\[CQ:\\w+[^\\[\\]]*]");
		Matcher cqMatcher = Pattern.compile("\\[CQ:\\w+[^\\[\\]]*]").matcher(messageStr);

		List<BotMessageChain> botMessageChain = new ArrayList<>();

		for (String s : textList) {
			if (StringUtils.isNotBlank(s)) {
				botMessageChain.add(BotMessageChain.ofPlain(s));
			}
			if (cqMatcher.find()) {
				findNextCqToChain(bot, botSender, cqMatcher, botMessageChain);
			}
		}

		while (cqMatcher.find()) {
			findNextCqToChain(bot, botSender, cqMatcher, botMessageChain);
		}
		return botMessageChain;
	}

	private void findNextCqToChain(BotRobot bot, BotSender botSender, Matcher cqMatcher, List<BotMessageChain> botMessageChain) {
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

	public void supplementChain(BotRobot bot, BotSender botSender, BotMessage respMessage) {
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

	// 转换CQ码文本为消息串
	// 例如 你好[CQ:at,qq=123]，我是[CQ:image,url=https://www.image.com/]，早安！
	// 转换后为 [{"type":"Plain","text":"你好"},{"type":"At","target":123},{"type":"Plain","text":"，我是"},{"type":"Image","url":"https://www.image.com/"},{"type":"Plain","text":"，早安！"}]
	public List<BotMessageChain> convertFunctionRespToChain(BotRobot bot, BotSender botSender, BotUserDTO botUser, String messageStr) {
		String[] textList = messageStr.split("(?<=])|(?=\\[)");

		List<BotMessageChain> botMessageChain = new ArrayList<>();

		for (String cq : textList) {
			String messageType = StringUtils.patten1("CQ:(\\w+)", cq);
			// 没有CQ就是文本
			if (StringUtils.isBlank(messageType)) {
				botMessageChain.add(BotMessageChain.ofPlain(cq));
				continue;
			}

			// 解析参数
			String[] paramList = cq.split(",=");
			Map<String, String> paramMap = new HashMap<>();
			for (int index = 1; index + 1 < paramList.length; index+=2) {
				paramMap.put(paramList[index], paramList[index + 1]);
			}

			String qq = paramMap.get("qq");
			BotUserDTO theUser = qq == null? null: botManager.addOrUpdateBotUser(bot, botSender, Long.valueOf(qq));
			BotUserDTO theUserOrSender = theUser == null? botUser: theUser;

			switch (messageType) {
				case "image": {
					Asserts.isTrue(paramMap.containsKey("url"), "啊嘞，不对劲");
					botMessageChain.add(BotMessageChain.ofImage(paramMap.get("url")));
					break;
				}
				case "at": {
					Asserts.notNull(theUser, "啊嘞，不对劲");
					botMessageChain.add(BotMessageChain.ofAt(theUser));
					break;
				}
				case "face": {
					Asserts.isTrue(paramMap.containsKey("id"), "啊嘞，不对劲");
					botMessageChain.add(BotMessageChain.ofFace(Integer.valueOf(paramMap.get("id"))));
					break;
				}
				case "memberName": {
					Asserts.notNull(theUserOrSender.getName(), "查无此人");
					botMessageChain.add(BotMessageChain.ofPlain(theUserOrSender.getName()));
					break;
				}
				case "portrait": {
					Asserts.notNull(theUserOrSender.getFace(), "查无此人");
					botMessageChain.add(BotMessageChain.ofImage(theUserOrSender.getFace()));
					break;
				}
				case "enter": botMessageChain.add(BotMessageChain.ofPlain("\n")); break;
				case "link": {
					Asserts.isTrue(paramMap.containsKey("url"), "啊嘞，不对劲");
					botMessageChain.add(BotMessageChain.ofLink(paramMap.get("url")));
					break;
				}
				default: throw new AssertException("cq正则解析异常, text=" + cq);
			}
		}

		return botMessageChain;
	}
}