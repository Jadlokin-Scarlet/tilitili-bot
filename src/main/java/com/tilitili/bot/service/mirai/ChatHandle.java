package com.tilitili.bot.service.mirai;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.nlp.v20190408.NlpClient;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotRequest;
import com.tencentcloudapi.nlp.v20190408.models.ChatBotResponse;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Component
public class ChatHandle extends ExceptionRespMessageHandle {
	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String text = messageAction.getParamOrDefault("提问", messageAction.getValue());
		Long group = messageAction.getBotSender().getGroup();
		Asserts.notBlank(text, "格式错啦(提问)");

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
		String reply = resp.getReply();

		if (Objects.equals(group, 674446384L)) {
			TimeUtil.millisecondsSleep(1000);
			return BotMessage.simpleListMessage(Arrays.asList(
					BotMessageChain.ofAt(2489917059L),
					BotMessageChain.ofPlain(reply)
			));
		}

		return BotMessage.simpleTextMessage(reply);
	}
}
