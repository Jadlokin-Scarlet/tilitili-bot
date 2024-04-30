package com.tilitili.bot.service.mirai.talk;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTalk;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotTalkManager;
import com.tilitili.common.manager.CheckManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TalkHandle extends ExceptionRespMessageHandle {
	private final CheckManager checkManager;
	private final BotTalkMapper botTalkMapper;
	private final BotTalkManager botTalkManager;
	private static final String reqKey = "TalkHandle.reqKey";
	private static final String addStatusKey = "TalkHandle.addStatusKey";
	private static final String delStatusKey = "TalkHandle.delStatusKey";
	private static final Gson gson = new Gson();

	@Autowired
	public TalkHandle(CheckManager checkManager, BotTalkMapper botTalkMapper, BotTalkManager botTalkManager) {
		this.checkManager = checkManager;
		this.botTalkMapper = botTalkMapper;
		this.botTalkManager = botTalkManager;
	}

	@Override
	public BotMessage handleAssertException(BotMessageAction messageAction, AssertException e) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		String senderReqKey = reqKey + userId;
		String senderStatusKey = addStatusKey + userId;

		session.remove(senderStatusKey);
		session.remove(senderReqKey);

		return super.handleAssertException(messageAction, e);
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "对话": return handleAdd(messageAction);
			case "移除对话": return handleDelete(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleDelete(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotMessage botMessage = messageAction.getBotMessage();
		BotUserDTO botUser = messageAction.getBotUser();
		String value = messageAction.getValue();
		String req = messageAction.getBodyOrDefault("提问", value);
		Long userId = botUser.getId();

		String senderStatusKey = delStatusKey + userId;

		List<String> imageList = messageAction.getImageList();
		BotTalk botTalk = null;
		if (session.containsKey(senderStatusKey)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(TalkHandle.convertMessageToString(botMessage), botMessage);
		} else if (StringUtils.isNotBlank(req)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(Gsons.toJson(BotMessage.simpleTextMessage(req)), botMessage);
		} else if (CollectionUtils.isNotEmpty(imageList)) {
			botTalk = botTalkManager.getJsonTalkOrOtherTalk(Gsons.toJson(BotMessage.simpleImageMessage(QQUtil.getImageUrl(imageList.get(0)))), botMessage);
		} else if (! session.containsKey(senderStatusKey)) {
			session.put(senderStatusKey, "1");
			return BotMessage.simpleTextMessage("请告诉我关键词吧");
		}
//		List<BotTalk> botTalkList = botTalkManager.getBotTalkByBotMessage(req, botMessage);
		session.remove(senderStatusKey);
		Asserts.notNull(botTalk, "没找到。");
		botTalkMapper.updateBotTalkSelective(new BotTalk().setId(botTalk.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("移除了。");
	}


	private BotMessage handleAdd(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotUserDTO botUser = messageAction.getBotUser();
		BotMessage botMessage = messageAction.getBotMessage();
		BotSender botSender = messageAction.getBotSender();
		List<String> imageList = messageAction.getImageList();
//		String sendType = botMessage.getSendType();
//		Long qq = botMessage.getQq();
//		Long group = botMessage.getGroup();
//		Long guildId = botMessage.getGuildId();
//		Long channelId = botMessage.getChannelId();
//		Long tinyId = botMessage.getTinyId();
		Long userId = botUser.getId();

		String senderReqKey = reqKey + userId;
		String senderStatusKey = addStatusKey + userId;

		String value = messageAction.getValue() == null? "": messageAction.getValue();
		String reqStr = messageAction.getBodyOrDefault("提问", value.contains(" ")? value.substring(0, value.indexOf(" ")).trim(): value);
		String respStr = messageAction.getBodyOrDefault("回答", value.contains(" ")? value.substring(value.indexOf(" ")).trim(): null);

		String req = null;
		String resp = null;
		int type = -1;

		if (session.containsKey(senderStatusKey)) {
			if (!session.containsKey(senderReqKey)) {
				Asserts.notEmpty(botMessage.getBotMessageChainList(), "关键词怎么是空的，不理你了。");
				session.put(senderReqKey, TalkHandle.convertMessageToString(botMessage));
				return BotMessage.simpleTextMessage("请告诉我回复吧");
			} else if (session.containsKey(senderReqKey)) {
				req = session.get(senderReqKey);
				Asserts.notEmpty(botMessage.getBotMessageChainList(), "回复怎么是空的，不理你了。");
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

		session.remove(senderStatusKey);
		session.remove(senderReqKey);

		Asserts.notBlank(req, "格式不对啦(提问)");
		Asserts.notBlank(resp, "格式不对啦(回答)");

		BotMessage respBotMessage = gson.fromJson(resp, BotMessage.class);
		String text = respBotMessage.getBotMessageChainList().stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_PLAIN))
				.map(BotMessageChain::getText).collect(Collectors.joining(""));
		Asserts.isTrue(checkManager.checkText(text), "达咩");

		BotTalk oldTalk = botTalkManager.getJsonTalkOrOtherTalk(req, botMessage);
		if (oldTalk != null) {
			botTalkMapper.deleteBotTalkByPrimary(oldTalk.getId());
		}

		BotTalk addBotTalk = new BotTalk().setType(type).setReq(req).setResp(resp).setSenderId(botSender.getId());
		botTalkMapper.addBotTalkSelective(addBotTalk);
		if (oldTalk == null) {
			List<BotMessageChain> messageChainList = Lists.newArrayList(BotMessageChain.ofPlain("学废了！"));
			messageChainList.addAll(respBotMessage.getBotMessageChainList());
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
		BotUserDTO botUser = botMessageAction.getBotUser();
		Long userId = botUser.getId();
		BotSessionService.MiraiSession session = botMessageAction.getSession();
		if (session.containsKey(addStatusKey + userId)) {
			return "对话";
		}
		if (session.containsKey(delStatusKey + userId)) {
			return "移除对话";
		}
		return null;
	}

	public static String convertMessageToString(BotMessage botMessage) {
		return gson.toJson(BotMessage.simpleListMessage(botMessage.getBotMessageChainList().stream()
				.filter(TalkHandle::needChain).peek(TalkHandle::suppleChain).collect(Collectors.toList())));
	}

	public static BotMessage convertStringToMessage(String botMessageStr) {
		return gson.fromJson(botMessageStr, BotMessage.class);
	}

	private static final List<String> needChainTypeList = Arrays.asList(BotMessage.MESSAGE_TYPE_PLAIN,BotMessage.MESSAGE_TYPE_IMAGE,BotMessage.MESSAGE_TYPE_AT,BotMessage.MESSAGE_TYPE_FACE);
	private static boolean needChain(BotMessageChain botMessageChain) {
		return needChainTypeList.contains(botMessageChain.getType());
	}

	private static void suppleChain(BotMessageChain botMessageChain) {
		if (Objects.equals(botMessageChain.getType(), BotMessage.MESSAGE_TYPE_IMAGE)) {
			botMessageChain.setUrl(QQUtil.getImageUrl(botMessageChain.getUrl()));
		}
	}

}
