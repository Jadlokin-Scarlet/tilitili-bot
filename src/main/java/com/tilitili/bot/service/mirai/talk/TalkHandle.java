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
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TalkHandle extends ExceptionRespMessageHandle {
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;
	private static final String reqKey = "TalkHandle.reqKey";
	private static final String statusKey = "TalkHandle.statusKey";
	private static final Gson gson = new Gson();

	@Autowired
	public TalkHandle(BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
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
		String reqStr = messageAction.getBodyOrDefault("提问", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): value);
		String respStr = messageAction.getBodyOrDefault("回答", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		String req = null;
		String resp = null;
		int type = -1;

		if (session.containsKey(senderStatusKey)) {
			if (!session.containsKey(senderReqKey)) {
				session.put(senderReqKey, TalkHandle.convertMessageToString(botMessage));
				return BotMessage.simpleTextMessage("请告诉我回复吧");
			} else if (session.containsKey(senderReqKey)) {
				req = session.get(senderReqKey);
				resp = TalkHandle.convertMessageToString(botMessage);
				type = 2;
			}
		} else if (StringUtils.isNotBlank(reqStr) && StringUtils.isNotBlank(respStr)) {
			req = gson.toJson(BotMessage.simpleTextMessage(reqStr));
			resp = gson.toJson(BotMessage.simpleTextMessage(respStr));
			type = 2;
		} else if (StringUtils.isBlank(reqStr) && imageList.size() == 1) {
			req = gson.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(0))));
			resp = gson.toJson(BotMessage.simpleTextMessage(respStr));
			type = 2;
		} else if (StringUtils.isBlank(respStr) && imageList.size() == 1) {
			req = gson.toJson(BotMessage.simpleTextMessage(reqStr));
			resp = gson.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(0))));
			type = 2;
		} else if (StringUtils.isBlank(reqStr) && StringUtils.isBlank(respStr) && imageList.size() == 2) {
			req = gson.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(0))));
			resp = gson.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(1))));
			type = 2;
		} else if (StringUtils.isBlank(value) && ! session.containsKey(senderStatusKey)) {
			session.remove(senderReqKey);
			session.put(senderStatusKey, "1");
			return BotMessage.simpleTextMessage("请告诉我关键词吧");
		}
		Asserts.notBlank(req, "格式不对(提问)");
		Asserts.notBlank(resp, "格式不对(回答)");

		session.remove(senderStatusKey);
		session.remove(senderReqKey);

		BotTalk oldTalk = botTalkManager.getJsonTalkOrOtherTalk(req, botMessage);
		if (oldTalk != null) {
			botTalkMapper.deleteBotTalkByPrimary(oldTalk.getId());
		}

		BotTalk addBotTalk = new BotTalk().setType(type).setReq(req).setResp(resp).setSendType(sendType).setSendQq(qq).setSendGroup(group).setSendGuild(guildId).setSendChannel(channelId).setSendTiny(tinyId);
		botTalkMapper.addBotTalkSelective(addBotTalk);
		if (oldTalk == null) {
			List<BotMessageChain> messageChainList = Lists.newArrayList(BotMessageChain.ofPlain("学废了！"));
			messageChainList.addAll(gson.fromJson(resp, BotMessage.class).getBotMessageChainList());
			return BotMessage.simpleListMessage(messageChainList);
		} else {
			List<BotMessageChain> messageChainList = Lists.newArrayList(BotMessageChain.ofPlain("覆盖了！原本是"));
			if (oldTalk.getType() == 0) {
				messageChainList.add(BotMessageChain.ofPlain(oldTalk.getResp()));
			} else if (oldTalk.getType() == 1) {
				messageChainList.add(BotMessageChain.ofImage(oldTalk.getResp()));
			} else if (oldTalk.getType() == 2) {
				messageChainList.addAll(gson.fromJson(oldTalk.getResp(), BotMessage.class).getBotMessageChainList());
			}
			return BotMessage.simpleListMessage(messageChainList);
		}
	}


	@Override
	public String isThisTask(BotMessageAction botMessageAction) {
		Long qqOrTinyId = botMessageAction.getQqOrTinyId();
		BotSessionService.MiraiSession session = botMessageAction.getSession();
		String senderStatusKey = statusKey + qqOrTinyId;
		if (session.containsKey(senderStatusKey)) {
			return "对话";
		}
		return null;
	}

	public static String convertMessageToString(BotMessage botMessage) {
		return gson.toJson(BotMessage.simpleListMessage(botMessage.getBotMessageChainList().stream()
				.filter(TalkHandle::needChain).collect(Collectors.toList())));
	}

	private static final List<String> needChainTypeList = Arrays.asList(BotMessage.MESSAGE_TYPE_PLAIN,BotMessage.MESSAGE_TYPE_IMAGE);
	private static boolean needChain(BotMessageChain botMessageChain) {
		return needChainTypeList.contains(botMessageChain.getType());
	}

}
