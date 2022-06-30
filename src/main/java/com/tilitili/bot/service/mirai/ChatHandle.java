package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson.JSONObject;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.nlp.v20190408.NlpClient;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotRequest;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotResponse;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.GroupEmum;
import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.StringUtils;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Component
public class ChatHandle extends ExceptionRespMessageHandle {
	@Value("${mirai.bot-qq}")
	private String BOT_QQ;
	@Value("${mirai.bot-guild-qq}")
	private String BOT_GUILD_QQ;
	private final static Random random = new Random(System.currentTimeMillis());

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String defaultSource = Objects.equals(messageAction.getBotMessage().getGroup(), GroupEmum.HOMO_LIVE_GROUP.value) ? "qy" : "tencent";
		String source = messageAction.getParamOrDefault("source", defaultSource);
		String text = messageAction.getText();
		int random = ChatHandle.random.nextInt(100);

		List<Long> atList = messageAction.getAtList();
		boolean isFriend = messageAction.getBotMessage().getSendType().equals(SendTypeEmum.FRIEND_MESSAGE_STR);
		boolean hasAtBot = atList.contains(Long.valueOf(BOT_QQ)) || atList.contains(Long.valueOf(BOT_GUILD_QQ));
		boolean isRandomReply = random == 0 && messageAction.getBotMessage().getSendType().equals(SendTypeEmum.GROUP_MESSAGE_STR);
		if (!isFriend && !hasAtBot && !isRandomReply) {
			return null;
		}

		Long group = messageAction.getBotSender().getGroup();
		if (StringUtils.isBlank(text)) {
			return null;
		}

		String reply = null;
		for (int index = 0; index < 10; index++) {
			switch (source) {
				case "tencent": reply = reqReply(text); break;
				case "qy": reply = reqQingYunReply(text); break;
			}
			if (StringUtils.isNotBlank(reply) && !reply.contains("小龙女")) break;
		}
		if (StringUtils.isBlank(reply) || reply.contains("小龙女")) return BotMessage.simpleTextMessage("网络似乎不太通常呢。");

		if (Objects.equals(group, 674446384L)) {
			TimeUtil.millisecondsSleep(1000);
			return BotMessage.simpleListMessage(Arrays.asList(
					BotMessageChain.ofAt(2489917059L),
					BotMessageChain.ofPlain(" "),
					BotMessageChain.ofPlain(reply)
			));
		}

		return BotMessage.simpleTextMessage(reply);
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
