package com.tilitili.bot.service.mirai.talk;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.QQUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;
	private static final String reqKey = "TalkHandle.reqKey";
	private static final String statusKey = "TalkHandle.statusKey";
	private final Gson gson;

	@Autowired
	public TalkHandle(BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
		gson = new Gson();
		this.botTalkMapper = botTalkMapper;
		this.botTalkManager = botTalkManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		List<String> imageList = messageAction.getImageList();
		String sendType = botMessage.getSendType();
		Long qq = botMessage.getQq();
		Long group = botMessage.getGroup();
		Long guildId = botMessage.getGuildId();
		Long channelId = botMessage.getChannelId();
		Long tinyId = botMessage.getTinyId();
		Long qqOrTinyId = messageAction.getQqOrTinyId();

		String senderReqKey = reqKey + qqOrTinyId;
		String senderStatusKey = statusKey + qqOrTinyId;

		String value = messageAction.getValueOrDefault("");
		String req = messageAction.getBodyOrDefault("提问", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): null);
		String resp = messageAction.getBodyOrDefault("回答", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		int type = -1;
		if (StringUtils.isBlank(req) && StringUtils.isBlank(resp) && imageList.size() == 2) {
			req = gson.toJson(Collections.singleton(BotMessageChain.ofImage(QQUtil.getImageUrl(imageList.get(0)))));
			resp = gson.toJson(Collections.singleton(BotMessageChain.ofImage(QQUtil.getImageUrl(imageList.get(1)))));
			type = 2;
		} else if (StringUtils.isBlank(req) && imageList.size() == 1) {
			req = gson.toJson(Collections.singleton(BotMessageChain.ofImage(QQUtil.getImageUrl(imageList.get(0)))));
			type = 2;
		} else if (StringUtils.isBlank(resp) && imageList.size() == 1) {
			resp = gson.toJson(Collections.singleton(BotMessageChain.ofImage(QQUtil.getImageUrl(imageList.get(0)))));
			type = 2;
		} else if (StringUtils.isBlank(req) && StringUtils.isBlank(resp) && StringUtils.isBlank(value)) {
			if (! session.containsKey(senderStatusKey)) {
				session.remove(senderReqKey);
				session.put(senderStatusKey, "1");
				return BotMessage.simpleTextMessage("请告诉我关键词吧");
			} else if (! session.containsKey(senderReqKey)) {
				session.put(senderReqKey, gson.toJson(botMessage));
				return BotMessage.simpleTextMessage("请告诉我回复吧");
			} else {
				BotMessage reqBotMessage = gson.fromJson(session.get(senderReqKey), BotMessage.class);
				req = this.convertMessageToString(reqBotMessage);
				resp = this.convertMessageToString(botMessage);
				type = 2;
			}
		}
		Asserts.notBlank(req, "格式不对(提问)");
		Asserts.notBlank(resp, "格式不对(回答)");

		session.remove(senderStatusKey);
		session.remove(senderReqKey);

		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, messageAction.getBotMessage());
		botTalkList.forEach(botTalk -> botTalkMapper.deleteBotTalkByPrimary(botTalk.getId()));

		BotTalk addBotTalk = new BotTalk().setType(type).setReq(req).setResp(resp).setSendType(sendType).setSendQq(qq).setSendGroup(group).setSendGuild(guildId).setSendChannel(channelId).setSendTiny(tinyId);
		botTalkMapper.addBotTalkSelective(addBotTalk);
		if (botTalkList.isEmpty()) {
			List<BotMessageChain> messageChainList = Lists.newArrayList(BotMessageChain.ofPlain("学废了！"));
			messageChainList.addAll(gson.fromJson(resp, BotMessage.class).getBotMessageChainList());
			return BotMessage.simpleListMessage(messageChainList);
		} else {
			List<BotMessageChain> messageChainList = Lists.newArrayList(BotMessageChain.ofPlain("覆盖了！原本是"));
			messageChainList.addAll(botTalkList.stream()
					.flatMap(talk -> {
						if (talk.getType() == 0) {
							return Stream.of(BotMessageChain.ofPlain(talk.getResp()));
						} else if (talk.getType() == 1) {
							return Stream.of(BotMessageChain.ofImage(talk.getResp()));
						} else if (talk.getType() == 2) {
							return gson.fromJson(talk.getResp(), BotMessage.class).getBotMessageChainList().stream();
						} else {
							return Stream.of();
						}
					}).collect(Collectors.toList()));
			return BotMessage.simpleListMessage(messageChainList);
		}
	}


	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		Long qqOrTinyId = botMessageAction.getQqOrTinyId();
		BotSessionService.MiraiSession session = botMessageAction.getSession();
		String senderStatusKey = statusKey + qqOrTinyId;
		if (! session.containsKey(senderStatusKey)) {
			return "对话";
		}
		return null;
	}

	private String convertMessageToString(BotMessage botMessage) {
		return gson.toJson(BotMessage.simpleListMessage(botMessage.getBotMessageChainList().stream()
				.skip(1)
				.filter(this::needChain).collect(Collectors.toList())));
	}

	private static final List<String> needChainTypeList = Arrays.asList(BotMessage.MESSAGE_TYPE_PLAIN,BotMessage.MESSAGE_TYPE_IMAGE);
	private boolean needChain(BotMessageChain botMessageChain) {
		return needChainTypeList.contains(botMessageChain.getType());
	}

}
