package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.mcping.McPingMod;
import com.tilitili.common.entity.view.bot.mcping.McPingResponse;
import com.tilitili.common.entity.view.request.MinecraftPlayer;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.McPingManager;
import com.tilitili.common.manager.MinecraftManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class McPingHandle extends ExceptionRespMessageHandle {
	private final String hostKey = "hostKey";

	private final McPingManager mcPingManager;
	private final MinecraftManager minecraftManager;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotRobotCacheManager botRobotCacheManager;

	@Autowired
	public McPingHandle(McPingManager mcPingManager, MinecraftManager minecraftManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderCacheManager botSenderCacheManager, BotRobotCacheManager botRobotCacheManager) {
		this.mcPingManager = mcPingManager;
		this.minecraftManager = minecraftManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.botRobotCacheManager = botRobotCacheManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws IOException {
		String key = messageAction.getKeyWithoutPrefix();
		String virtualKey = messageAction.getVirtualKey();
		switch (virtualKey != null? virtualKey: key) {
			case "mcp": case "mcpd": case "mcm": case "在线人数": return handleMcp(messageAction);
			case "mcl": case "在线玩家": return handelMcList(messageAction);
			default: return null;
		}
	}

	private BotMessage handelMcList(BotMessageAction messageAction) {
		BotRobot bot = messageAction.getBot();
		Long senderId = messageAction.getBotSender().getId();
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(senderId));
		for (BotForwardConfig forwardConfig : forwardConfigList) {
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetBotSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
			BotRobot minecraftBot = botRobotCacheManager.getValidBotRobotById(targetBotSender.getSendBot());

			List<MinecraftPlayer> playerList = minecraftManager.listOnlinePlayer(minecraftBot);
			String playerListStr = playerList.stream().map(MinecraftPlayer::getDisplayName).map(minecraftManager::trimMcName).collect(Collectors.joining("，"));
			return BotMessage.simpleTextMessage("当前在线玩家："+playerListStr);
		}
//		if (senderId == 3758L) {
//			List<MinecraftPlayer> playerList = minecraftManager.listOnlinePlayer(MinecraftServerEnum.RANK_CHANNEL_MINECRAFT);
//			String playerListStr = playerList.stream().map(MinecraftPlayer::getDisplayName).map(minecraftManager::trimMcName).collect(Collectors.joining("，"));
//			return BotMessage.simpleTextMessage("当前在线玩家："+playerListStr);
//		}
		return null;
	}

	private BotMessage handleMcp(BotMessageAction messageAction) throws IOException {
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
		String url2 = url.contains(":")? url: url + ":25565";
		if (!url2.contains(":")) url2 += ":25565";
//		Asserts.isTrue(url2.contains(":"), "地址格式不对");
		String host = url2.substring(0, url2.indexOf(":"));
		String port = url2.substring(url2.indexOf(":") + 1);
		McPingResponse response;
		try {
			response = mcPingManager.mcPing(new InetSocketAddress(host, Integer.parseInt(port)));
		} catch (SocketTimeoutException | ConnectException e) {
			throw new AssertException("网络异常", e);
		}
		Asserts.notNull(response, "服务器不在线");
		Integer onlinePlayerCnt = response.getPlayers().getOnline();
		String version = response.getVersion().getName();
		List<BotMessageChain> chainList = new ArrayList<>();
		chainList.add(BotMessageChain.ofPlain(String.format("服务器在线！版本%s, 在线玩家数：%s，地址: ", version, onlinePlayerCnt)));
		chainList.add(BotMessageChain.ofLink(url));
		if (key.equals("mcm")) {
			String modListStr = response.getModinfo().getModList().stream().map(McPingMod::getModid).collect(Collectors.joining(","));
			chainList.add(BotMessageChain.ofPlain(String.format("%nmod列表：%s", modListStr)));
		}
		return BotMessage.simpleListMessage(chainList);
	}
}
