package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.nlp.v20190408.NlpClient;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotRequest;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotResponse;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.AnimeWordsService;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.BotEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Slf4j
@Component
public class ChatHandle extends ExceptionRespMessageHandle {
	@Value("${mirai.bot-guild-qq}")
	private String BOT_GUILD_QQ;
	private final static Random random = new Random(System.currentTimeMillis());
	private final Gson gson = new Gson();
	private final BotManager botManager;
	private final AnimeWordsService animeWordsService;
	private final List<String> nameList = Arrays.asList("tx", "qy", "ml", "qln");
	public static final String nameKey = "ChatHandle.nameKey";

	@Autowired
	public ChatHandle(BotManager botManager, AnimeWordsService animeWordsService) {
		this.botManager = botManager;
		this.animeWordsService = animeWordsService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String key = messageAction.getKeyWithoutPrefix();
		switch (key) {
			case "换人": return handleChange(messageAction);
			default: return handleChat(messageAction);
		}
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

	private BotMessage handleChat(BotMessageAction messageAction) throws TencentCloudSDKException, UnsupportedEncodingException {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String redisKey = nameKey + messageAction.getBotSender().getId();
		String source = session.getOrDefault(redisKey, "tx");
		if (source.equals("qln")) {
			return null;
		}

		BotEmum bot = messageAction.getBot();
		if (bot == null) {
			return null;
		}
//		String defaultSource = Objects.equals(messageAction.getBotMessage().getGroup(), GroupEmum.HOMO_LIVE_GROUP.value) ? "qy" : "tx";
//		String source = messageAction.getParamOrDefault("source", defaultSource);
		String text = messageAction.getText();
		int random = ChatHandle.random.nextInt(500);

		List<Long> atList = messageAction.getAtList();
		atList.retainAll(BotUserConstant.BOT_USER_ID_LIST);
		boolean isFriend = messageAction.getBotSender().getSendType().equals(SendTypeEmum.FRIEND_MESSAGE_STR);
		boolean hasAtBot = !atList.isEmpty();
		boolean isRandomReply = random == 0 && messageAction.getBotSender().getSendType().equals(SendTypeEmum.GROUP_MESSAGE_STR);
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
					reply = reqReply(text);
					if (StringUtils.isNotBlank(reply) && !reply.contains("小龙女")) break;
				}
				if (StringUtils.isBlank(reply) || reply.contains("小龙女")) return BotMessage.simpleTextMessage("网络似乎不太通常呢。");
				if (isRandomReply && (reply.contains("能再说一遍么") || reply.contains("不是姑姑我不明白，这世界变化快"))) {
					return BotMessage.emptyMessage();
				}
				break;
			}
			case "qy": reply = reqQingYunReply(text); break;
			case "ml": chainList = reqMoLiReply(text, messageAction); break;
		}

//		if (Objects.equals(group, 674446384L)) {
//			TimeUtil.millisecondsSleep(1000);
//			return BotMessage.simpleListMessage(Arrays.asList(
//					BotMessageChain.ofAt(2489917059L),
//					BotMessageChain.ofPlain(" "),
//					BotMessageChain.ofPlain(reply)
//			));
//		}
		if (chainList != null) {
			return BotMessage.simpleListMessage(chainList);
		} else if (StringUtils.isNotBlank(reply)) {
			return BotMessage.simpleTextMessage(reply);
		} else {
			return null;
		}
	}

	private final static Map<String, String> header = ImmutableMap.of("Api-Key", "g1cpoxzjnw6teqjr", "Api-Secret", "7bmdopdk");
	private List<BotMessageChain> reqMoLiReply(String text, BotMessageAction messageAction) {
		// 假装吧词库导进了茉莉云
		String wordReply = animeWordsService.getRespByShortContain(text);
		if (StringUtils.isNotBlank(wordReply)) {
			return Lists.newArrayList(BotMessageChain.ofPlain(wordReply));
		}
		BotEmum bot = messageAction.getBot();
		BotMessage botMessage = messageAction.getBotMessage();
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		ImmutableMap.Builder<Object, Object> param;
		if (Objects.equals(botSender.getSendType(), SendTypeEmum.FRIEND_MESSAGE_STR)) {
			param = ImmutableMap.builder().put("content", text)
					.put("type", 1)
					.put("from", botUser.getQq())
					.put("fromName", botUser.getName());
		} else if (Objects.equals(botSender.getSendType(), SendTypeEmum.GROUP_MESSAGE_STR)) {
			param = ImmutableMap.builder().put("content", text)
					.put("type", 2)
					.put("from", botUser.getQq())
					.put("fromName", botUser.getName())
					.put("to", botSender.getGroup());
			if (botSender.getName() != null) param.put("toName", botSender.getName());
		} else {
			throw new AssertException();
		}
		String json = gson.toJson(param.build());
		String respStr = HttpClientUtil.httpPost("https://api.mlyai.com/reply", json, header);
		JSONObject resp = JSONObject.parseObject(respStr);
		log.debug("请求茉莉 req={} result={}", json, respStr);
		Asserts.notEquals(resp.getInteger("code"), "C1001", "呜呜呜被关小黑屋了。");
		Asserts.checkEquals(resp.getString("code"), "00000", "不对劲");

		JSONArray replyList = resp.getJSONArray("data");
		List<BotMessageChain> chainList = new ArrayList<>();
		for (int index = 0; index < replyList.size(); index++) {
			JSONObject reply = replyList.getJSONObject(index);
			Integer typed = reply.getInteger("typed");
			String content = reply.getString("content");
			if (typed == null) continue;
			switch (typed) {
				case 1: chainList.add(BotMessageChain.ofPlain(content)); break;
				case 2: chainList.add(BotMessageChain.ofImage("https://files.molicloud.com/" + content)); break;
				case 3: log.error("记录文档, content={}", content);
				case 4: chainList.add(BotMessageChain.ofVoice(botManager.uploadVoice(bot, "https://files.molicloud.com/" + content)));
				case 8: log.error("记录json, json={}", content);
				case 9: log.error("记录其他文件, content={}", content);
				default: log.error("记录其他情况, content={}", content);
			}
		}
		return chainList;
	}

	private String reqQingYunReply(String text) throws UnsupportedEncodingException {
		String respStr = HttpClientUtil.httpGet("http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + URLEncoder.encode(text, "UTF-8"));
		JSONObject resp = JSONObject.parseObject(respStr);
		log.debug("请求青云 req={} result={}", text, respStr);
		Asserts.checkEquals(resp.getInteger("result"), 0, "不对劲");

		return resp.getString("content").replaceAll("\\{br}", "\n");
	}

	private String reqReply(String text) throws TencentCloudSDKException {
		// 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
		// 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
		Credential cred = new Credential("AKIDYzDDyeCWbREubHpAH8kCYidKmzwe8Rba", "qRntjJcgX9WdV8iGlDXcS3aw53oNincD");
		// 实例化一个http选项，可选的，没有特殊需求可以跳过
		HttpProfile httpProfile = new HttpProfile();
		httpProfile.setEndpoint("nlp.tencentcloudapi.com");
		// 实例化一个client选项，可选的，没有特殊需求可以跳过
		ClientProfile clientProfile = new ClientProfile();
		clientProfile.setHttpProfile(httpProfile);
		// 实例化要请求产品的client对象,clientProfile是可选的
		NlpClient client = new NlpClient(cred, "ap-guangzhou", clientProfile);
		// 实例化一个请求对象,每个接口都会对应一个request对象
		ChatBotRequest req = new ChatBotRequest();
		req.setQuery(text);
		// 返回的resp是一个ChatBotResponse的实例，与请求对象对应
		ChatBotResponse resp = client.ChatBot(req);
		return resp.getReply();
	}
}
