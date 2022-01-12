package com.tilitili.bot.service.mirai;

import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mcsrvstat.McsrvstatResponse;
import com.tilitili.common.manager.McsrvstatManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McPingHandle extends ExceptionRespMessageHandle {
	private final String hostKey = "hostKey";

	private final McsrvstatManager mcsrvstatManager;

	@Autowired
	public McPingHandle(McsrvstatManager mcsrvstatManager) {
		this.mcsrvstatManager = mcsrvstatManager;
	}

	@Override
	public MessageHandleEnum getType() {
		return MessageHandleEnum.MC_PING_HANDLE;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String key = messageAction.getKey();
		Asserts.notBlank(key, "key是啥");
		if (key.equals("mcpd")) {
			String value = messageAction.getValue();
			Asserts.notBlank(value, "格式错啦(地址)");
			session.put(hostKey, value);
			return BotMessage.simpleTextMessage("记住啦！下次mcp不用加地址了。");
		}
		String host = messageAction.getValueOrDefault(session.get(hostKey));
		Asserts.notBlank(host, "没有地址");
		McsrvstatResponse response = mcsrvstatManager.mcping(host);
		Boolean online = response.getOnline();
		Asserts.isTrue(online, "服务器不在线");
		Integer onlinePlayerCnt = response.getPlayers().getOnline();
		String version = response.getVersion();
		String message = String.format("服务器在线！版本%s, 在线玩家数：%s", version, onlinePlayerCnt);
		if (key.equals("mcm")) {
			message += String.format("\nmod列表：%s", String.join(",", response.getMods().getNames()));
		}
		return BotMessage.simpleTextMessage(message);
	}
}
