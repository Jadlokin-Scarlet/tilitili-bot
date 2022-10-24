package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageNode;
import com.tilitili.common.mapper.mysql.BotUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ForwardMarkHandle extends ExceptionRespMessageToSenderHandle {
	private final BotUserMapper botUserMapper;
	private final FunctionTalkService functionTalkService;

	@Autowired
	public ForwardMarkHandle(BotUserMapper botUserMapper, FunctionTalkService functionTalkService) {
		this.botUserMapper = botUserMapper;
		this.functionTalkService = functionTalkService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String body = messageAction.getBody();
		Asserts.notBlank(body, "格式错啦(台本)");
		String[] rowList = body.split("\n");

		List<BotMessageNode> nodeList = new ArrayList<>();
		for (int i = 0; i < rowList.length; i++) {
			String row = rowList[i];
			List<String> cellList = StringUtils.extractList("(\\d+)[：:](.+)", row);
			Asserts.checkEquals(cellList.size(), 2, "第%s句格式错啦", i);
			Long senderId = Long.parseLong(cellList.get(0));
			String text = cellList.get(1);

			BotUser botUser = botUserMapper.getBotUserByExternalId(senderId);
			Asserts.checkEquals(cellList.size(), 2, "第%s句找不到人", i);

			String senderName = botUser.getName();
			nodeList.add(new BotMessageNode().setSenderId(senderId).setSenderName(senderName).setMessageChain(
					functionTalkService.convertCqToMessageChain(text)
			));
		}
		return BotMessage.simpleForwardMessage(nodeList);
	}
}
