package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.AnimeWordsService;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotMessageRecordQuery;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.MoliManager;
import com.tilitili.common.manager.OpenAiManager;
import com.tilitili.common.manager.TencentCloudApiManager;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatHandle extends ExceptionRespMessageHandle {
	private final AnimeWordsService animeWordsService;
	private final OpenAiManager openAiManager;
	private final MoliManager moliManager;
	private final BotRobotCacheManager botRobotCacheManager;
	private final TencentCloudApiManager tencentCloudApiManager;
	private final BotMessageRecordMapper botMessageRecordMapper;

	private final static Random random = new Random(System.currentTimeMillis());
	private final List<String> nameList = Arrays.asList("tx", "qy", "ml", "ai", "aig");
	public static final String nameKey = "ChatHandle.nameKey";
	private static final String networkKey = "ChatHandle.networkKey";

	@Autowired
	public ChatHandle(AnimeWordsService animeWordsService, OpenAiManager openAiManager, TencentCloudApiManager tencentCloudApiManager, MoliManager moliManager, BotRobotCacheManager botRobotCacheManager, BotMessageRecordMapper botMessageRecordMapper) {
		this.animeWordsService = animeWordsService;
		this.openAiManager = openAiManager;
		this.moliManager = moliManager;
		this.botRobotCacheManager = botRobotCacheManager;
		this.tencentCloudApiManager = tencentCloudApiManager;
		this.botMessageRecordMapper = botMessageRecordMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "换人": return handleChange(messageAction);
			case "重置": return handleRefresh(messageAction);
			case "联网": return handleNetwork(messageAction);
			default: return handleChat(messageAction);
		}
	}

	private BotMessage handleNetwork(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		BotSender botSender = messageAction.getBotSender();
		String redisKey = nameKey + botSender.getId();
		String source = session.getOrDefault(redisKey, "tx");

		if (!"ai".equals(source)) {
			return null;
		}

		boolean network = "true".equals(session.getOrDefault(networkKey, "false"));
		if (network) {
			session.put(networkKey, "false");
			return BotMessage.simpleTextMessage("关闭喵");
		} else {
			session.put(networkKey, "true");
			return BotMessage.simpleTextMessage("冲浪喵");
		}
	}

	private BotMessage handleRefresh(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		openAiManager.refreshFreeChat(senderId);
		return BotMessage.simpleTextMessage("拜拜喵");
	}

	private BotMessage handleChange(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String name = messageAction.getValue();
		Asserts.isTrue(nameList.contains(name), "你要换谁鸭");

		String redisKey = nameKey + messageAction.getBotSender().getId();
		if ("tx".equals(name)) {
			session.remove(redisKey);
		} else {
			session.put(redisKey, name);
		}
		return BotMessage.simpleTextMessage("√");
	}

	private final List<String> randomSendTypeList = Arrays.asList(SendTypeEnum.GROUP_MESSAGE_STR, SendTypeEnum.KOOK_MESSAGE_STR, SendTypeEnum.MINECRAFT_MESSAGE_STR);
	private BotMessage handleChat(BotMessageAction messageAction) throws TencentCloudSDKException, UnsupportedEncodingException {
		BotSessionService.MiraiSession session = messageAction.getSession();
		boolean network = "true".equals(session.getOrDefault(networkKey, "false"));
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		String redisKey = nameKey + botSender.getId();
		String source = session.getOrDefault(redisKey, "tx");

		BotRobot bot = messageAction.getBot();
		if (bot == null) {
			return null;
		}
//		String defaultSource = Objects.equals(messageAction.getBotMessage().getGroup(), GroupEnum.HOMO_LIVE_GROUP.value) ? "qy" : "tx";
//		String source = messageAction.getParamOrDefault("source", defaultSource);
		String text = messageAction.getText();
		int random = ChatHandle.random.nextInt(300);
		List<Long> botList = botRobotCacheManager.getBotRobotByCondition(new BotRobotQuery().setStatus(0)).stream().map(BotRobot::getUserId).filter(Objects::nonNull).collect(Collectors.toList());

		List<Long> atList = messageAction.getAtList().stream().map(BotUserDTO::getId).collect(Collectors.toList());
		atList.retainAll(botList);
		boolean isFriend = botSender.getSendType().equals(SendTypeEnum.FRIEND_MESSAGE_STR);
		boolean hasAtBot = !atList.isEmpty();
		boolean isRandomReply = random == 0 && randomSendTypeList.contains(botSender.getSendType());
		if (!isFriend && !hasAtBot && !isRandomReply) {
			return null;
		}

//		Long group = messageAction.getBotSender().getGroup();
		if (StringUtils.isBlank(text)) {
			return null;
		}

		String reply = null;
		List<BotMessageChain> chainList = null;
		switch (source) {
			case "tx": {
				if ("签到".equals(text.trim())) {
					break;
				}
				for (int index = 0; index < 3; index++) {
					reply = tencentCloudApiManager.reqReply(text);
					if (StringUtils.isNotBlank(reply) && !reply.contains("小龙女") && !reply.contains("再说一遍")) break;
					TimeUtil.millisecondsSleep(100);
				}
				if (StringUtils.isBlank(reply) || reply.contains("小龙女")) return BotMessage.simpleTextMessage("网络似乎不太通常呢。");
				if (isRandomReply && (reply.contains("能再说一遍么") || reply.contains("不是姑姑我不明白，这世界变化快") || reply.contains("再说一遍可以么"))) {
					return BotMessage.emptyMessage();
				}
				break;
			}
			case "qy": reply = reqQingYunReply(text); break;
			case "ml": chainList = reqMoLiReply(text, messageAction); break;
			case "ai": {
				Date endTime = new Date();// endTime = DateUtils.parseDateYMDHMS("2023-05-30 03:01:00")
				Date startTime = DateUtils.addTime(endTime, Calendar.MINUTE, -2);
				List<BotMessageRecord> messageRecordList = isRandomReply? botMessageRecordMapper.getBotMessageRecordByCondition(new BotMessageRecordQuery().setSenderId(botSender.getId()).setCreateTimeStart(startTime).setCreateTimeEnd(endTime)): Collections.emptyList();
				List<BotMessageRecord> filterMessageRecordList = messageRecordList.stream().filter(StreamUtil.isEqual(BotMessageRecord::getMessageId, messageAction.getMessageId()).negate()).collect(Collectors.toList());
				reply = openAiManager.freeChat(botSender, messageAction.getBotMessage(), network, filterMessageRecordList);
				break;
			}
			case "aig": chainList = openAiManager.chatGlm(bot, botUser, text); break;
		}

		if (chainList != null) {
			return BotMessage.simpleListMessage(chainList);
		} else if (StringUtils.isNotBlank(reply)) {
			return BotMessage.simpleTextMessage(reply);
		} else {
			return null;
		}
	}

	private List<BotMessageChain> reqMoLiReply(String text, BotMessageAction messageAction) {
		// 假装吧词库导进了茉莉云
		String wordReply = animeWordsService.getRespByShortContain(text);
		if (StringUtils.isNotBlank(wordReply)) {
			return Lists.newArrayList(BotMessageChain.ofPlain(wordReply));
		}
		BotRobot bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		return moliManager.reqMoLiReply(text, bot, botSender, botUser);
	}

	private String reqQingYunReply(String text) throws UnsupportedEncodingException {
		String respStr = HttpClientUtil.httpGet("http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + URLEncoder.encode(text, "UTF-8"));
		JSONObject resp = JSONObject.parseObject(respStr);
		log.debug("请求青云 req={} result={}", text, respStr);
		Asserts.checkEquals(resp.getInteger("result"), 0, "不对劲");

		return resp.getString("content").replaceAll("\\{br}", "\n");
	}

}
