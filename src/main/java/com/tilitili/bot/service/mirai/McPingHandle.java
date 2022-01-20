package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.mcping.McPingMod;
import com.tilitili.common.entity.view.bot.mcping.McPingResponse;
import com.tilitili.common.manager.McPingManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

@Component
public class McPingHandle extends ExceptionRespMessageHandle {
	private final String hostKey = "hostKey";

	private final McPingManager mcPingManager;

	@Autowired
	public McPingHandle(McPingManager mcPingManager) {
		this.mcPingManager = mcPingManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws IOException {
		BotSessionService.MiraiSession session = messageAction.getSession();
		String key = messageAction.getKeyWithoutPrefix();
		Asserts.notBlank(key, "key是啥");
		if (key.equals("mcpd")) {
			String value = messageAction.getValue();
			Asserts.notBlank(value, "格式错啦(地址)");
			session.put(hostKey, value);
			return BotMessage.simpleTextMessage("记住啦！下次mcp不用加地址了。");
		}
		String url = messageAction.getValueOrDefault(session.get(hostKey));
		Asserts.notBlank(url, "没有地址");
		Asserts.isTrue(url.contains(":"), "地址格式不对");
		String host = url.substring(0, url.indexOf(":"));
		String port = url.substring(url.indexOf(":") + 1);
		McPingResponse response = mcPingManager.mcPing(new InetSocketAddress(host, Integer.parseInt(port)));
		Asserts.notNull(response, "服务器不在线");
		Integer onlinePlayerCnt = response.getPlayers().getOnline();
		String version = response.getVersion().getName();
		String message = String.format("服务器在线！版本%s, 在线玩家数：%s，地址: %s", version, onlinePlayerCnt, url);
		if (key.equals("mcm")) {
			String modListStr = response.getModinfo().getModList().stream().map(McPingMod::getModid).collect(Collectors.joining(","));
			message += String.format("\nmod列表：%s", modListStr);
		}
		return BotMessage.simpleTextMessage(message);
	}
}
