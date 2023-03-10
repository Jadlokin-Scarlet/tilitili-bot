package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.BotMessageNode;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ForwardMarkHandle extends ExceptionRespMessageToSenderHandle {
	private final BotUserManager botUserManager;
	private final FunctionTalkService functionTalkService;

	@Autowired
	public ForwardMarkHandle(BotUserManager botUserManager, FunctionTalkService functionTalkService) {
		this.botUserManager = botUserManager;
		this.functionTalkService = functionTalkService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotSender botSender = messageAction.getBotSender();
		String body = messageAction.getBody();
		Asserts.notBlank(body, "格式错啦(台本)");
		List<BotMessageNode> nodeList = getForwardMessageByText(botSender, body);
		return BotMessage.simpleForwardMessage(nodeList);
	}

	private List<BotMessageNode> getForwardMessageByText(BotSender botSender, String body) {
		String[] rowList = body.split("\n");

		List<BotMessageNode> nodeList = new ArrayList<>();
		for (int i = 0; i < rowList.length; i++) {
			String row = rowList[i];
			List<String> cellList = StringUtils.extractList("(\\d+)[：:](.+)", row);
			Asserts.checkEquals(cellList.size(), 2, "第%s句格式错啦", i);
			long qq = Long.parseLong(cellList.get(0));
			String text = cellList.get(1);
			List<BotMessageChain> botMessageChains = functionTalkService.convertCqToMessageChain(botSender, text);

			// 此功能只能QQ用
			BotUserDTO botUser = botUserManager.getBotUserByExternalIdWithParent(qq, 0);
			Asserts.notNull(botUser, "第%s句找不到人", i);
			nodeList.add(new BotMessageNode().setUser(botUser).setSenderName(botUser.getName()).setMessageChain(botMessageChains));
		}
		return nodeList;
	}
}
