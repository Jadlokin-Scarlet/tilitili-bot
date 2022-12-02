package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.CtsManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class CtsHandle extends BaseMessageHandleAdapt {
	private final CtsManager ctsManager;

	private final Random random;

	@Autowired
	public CtsHandle(CtsManager ctsManager) {
		this.ctsManager = ctsManager;

		this.random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String worlds = messageAction.getValue();
		Asserts.notBlank(worlds, "格式错啦(内容)");
		List<String> keyList = StringUtils.extractList("藏(头|尾)诗(五言|七言)?", messageAction.getKeyWithoutPrefix());
		Asserts.checkEquals(keyList.size(), 2, "格式错啦");
		String type = "头".equals(keyList.get(0))? "start": "end";
		Integer num = "七言".equals(keyList.get(1))? 7: 5;
		List<String> ctsList = ctsManager.getCts(worlds, num, type);
		return BotMessage.simpleTextMessage(ctsList.get(random.nextInt(ctsList.size())));
	}
}
