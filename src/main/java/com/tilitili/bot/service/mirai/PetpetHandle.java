package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.component.CloseableTempFile;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.petpet.PetpetData;
import com.tilitili.common.entity.view.bot.petpet.PetpetRequest;
import com.tilitili.common.entity.view.bot.petpet.PetpetRequestUser;
import com.tilitili.common.entity.view.bot.petpet.PetpetResponse;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.OSSUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PetpetHandle extends ExceptionRespMessageHandle {
	@Value("${petpet.host:http://172.27.0.7:2333/petpet}")
	private String petpetHost;

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String value = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		List<BotUserDTO> atList = messageAction.getAtList();
		Asserts.notBlank(value, "格式错啦(表情包)");

		String key;
		String text;
		if (value.contains(" ")) {
			key = value.substring(0, value.indexOf(" ")).trim();
			text = value.substring(value.indexOf(" ")).trim();
		} else {
			key = value;
			text = null;
		}

		List<PetpetData> petpetDataList = this.listPetpet();
		PetpetData data = petpetDataList.stream().filter(item -> item.getAlias().contains(key) || item.getKey().equals(key)).findFirst()
				.orElseThrow(() -> new AssertException("未找到表情包，发送[帮助 生成]查看详情"));

		PetpetRequest petpetRequest = new PetpetRequest().setKey(data.getKey());
		for (String type : data.getTypes().stream().distinct().collect(Collectors.toList())) {
			BotUserDTO theUser;
			if (petpetRequest.getTo() == null && petpetRequest.getFrom() == null) {
				theUser = atList.isEmpty() ? botUser : atList.get(0);
			} else {
				Asserts.isTrue(atList.size() > 1, "该表情包需要@2人");
				theUser = atList.get(1);
			}
			Asserts.notNull(theUser.getFace(), "找不到头像");
			PetpetRequestUser requestUser = new PetpetRequestUser().setName(theUser.getName()).setAvatar(theUser.getFace());
			if ("TO".equals(type)) {
				petpetRequest.setTo(requestUser);
			} else {
				petpetRequest.setFrom(requestUser);
			}
		}

		if (text != null) {
			petpetRequest.setTextList(Collections.singletonList(text));
		}
		String img = generate(petpetRequest);
		return BotMessage.simpleImageMessage(img);
	}

	private List<PetpetData> listPetpet() {
		String result = HttpClientUtil.httpGet(petpetHost);
		Asserts.notBlank(result, "网络异常");
		PetpetResponse response = Gsons.fromJson(result, PetpetResponse.class);
		Asserts.notNull(response, "网络异常");
		Asserts.notEmpty(response.getPetData(), "网络异常");
		return response.getPetData();
	}

	private String generate(PetpetRequest request) {
		try (CloseableTempFile file = CloseableTempFile.ofUrlPost(petpetHost, Gsons.toJson(request))) {
			return OSSUtil.uploadOSSByFile(file.getFile());
		} catch (IOException e) {
			throw new AssertException("网络异常");
		}
	}
}
