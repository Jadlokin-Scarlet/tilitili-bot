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

@Component
public class PetpetHandle extends ExceptionRespMessageHandle {
	@Value("${petpet.host:http://172.27.0.7:2333/petpet}")
	private String petpetHost;

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String value = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		List<BotUserDTO> atList = messageAction.getAtList();
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
		PetpetData data = petpetDataList.stream().filter(item -> item.getAlias().contains(key)).findFirst()
				.orElseThrow(() -> new AssertException("未找到标签，发送[帮助 生成]查看详情"));

		PetpetRequest petpetRequest = new PetpetRequest().setKey(data.getKey());
		BotUserDTO firstUser = atList.isEmpty()? botUser: atList.get(0);
		Asserts.notNull(firstUser.getFace(), "找不到头像");
		petpetRequest.setTo(new PetpetRequestUser().setName(firstUser.getName()).setAvatar(firstUser.getFace()));
		petpetRequest.setTextList(Collections.singletonList(text));
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
