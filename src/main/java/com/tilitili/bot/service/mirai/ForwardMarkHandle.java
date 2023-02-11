package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
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
		List<BotMessageNode> nodeList = getForwardMessageByText(botSender, body, null);
		return BotMessage.simpleForwardMessage(nodeList);
	}

	public List<BotMessageNode> getForwardMessageByText(BotSender botSender, String body, String customBotName) {
		String[] rowList = body.split("\n");

		List<BotMessageNode> nodeList = new ArrayList<>();
		for (int i = 0; i < rowList.length; i++) {
			String row = rowList[i];
			List<String> cellList = StringUtils.extractList("(\\d+)[：:](.+)", row);
			Asserts.checkEquals(cellList.size(), 2, "第%s句格式错啦", i);
			long qq = Long.parseLong(cellList.get(0));
			String text = cellList.get(1);

			// 此功能只能QQ用
			BotUserDTO botUser;
			String senderName;
			if (qq == 0) {
				botUser = null;
				senderName = "旁白";
			} else {
				botUser = botUserManager.getBotUserByExternalIdWithParent(qq, 0);
				Asserts.notNull(botUser, "第%s句找不到人", i);
				if (BotUserConstant.BOT_USER_ID_LIST.contains(botUser.getId())) {
					senderName = customBotName;
				} else {
					senderName = botUser.getName();
				}
			}
			Asserts.notNull(senderName, "啊嘞，不对劲");
			nodeList.add(new BotMessageNode().setUserId(botUser == null? null: botUser.getId()).setSenderName(senderName).setMessageChain(
					functionTalkService.convertCqToMessageChain(botSender, text)
			));
		}
		return nodeList;
	}
}
