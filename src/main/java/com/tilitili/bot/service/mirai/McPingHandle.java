package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.emnus.SendTypeEnum;
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
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class McPingHandle extends ExceptionRespMessageHandle {
	private final String hostKey = "hostKey";

	private final McPingManager mcPingManager;
	private final MinecraftManager minecraftManager;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotRobotCacheManager botRobotCacheManager;
	private final BotConfigManager botConfigManager;
	private final BotManager botManager;
	private final BotRoleManager botRoleManager;

	@Autowired
	public McPingHandle(McPingManager mcPingManager, MinecraftManager minecraftManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderCacheManager botSenderCacheManager, BotRobotCacheManager botRobotCacheManager, BotConfigManager botConfigManager, BotManager botManager, BotRoleManager botRoleManager) {
		this.mcPingManager = mcPingManager;
		this.minecraftManager = minecraftManager;
		this.botForwardConfigMapper = botForwardConfigMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.botRobotCacheManager = botRobotCacheManager;
		this.botConfigManager = botConfigManager;
		this.botManager = botManager;
		this.botRoleManager = botRoleManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws IOException {
		String key = messageAction.getKeyWithoutPrefix();
		String virtualKey = messageAction.getKeyWithoutPrefix();
		switch (virtualKey != null? virtualKey: key) {
			case "mcp": case "mcpd": case "mcm": case "在线人数": return handleMcp(messageAction);
			case "mcl": case "在线玩家": return handelMcList(messageAction);
			case "mc白名单": return handleWhiteList(messageAction);
			case "mcBind": return handleBind(messageAction);
			default: return null;
		}
	}

	private BotMessage handleBind(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		String bindSenderIdSrt = messageAction.getValue();
		Asserts.isTrue(botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser()), "权限不足");
		Asserts.isNumber(bindSenderIdSrt, "格式错啦(服务器id)");
		Long bindSenderId = Long.valueOf(bindSenderIdSrt);
		BotSender bindSender = botSenderCacheManager.getValidBotSenderById(bindSenderId);
		Asserts.notNull(bindSender, "服务器不存在");
		Asserts.checkEquals(bindSender.getSendType(), SendTypeEnum.MINECRAFT_MESSAGE_STR, "服务器不存在");
		botConfigManager.addOrUpdateSenderConfig(senderId, "mcBind", bindSenderId);
		return BotMessage.simpleTextMessage("成功绑定服务器"+bindSender.getName());
	}

	private BotMessage handleWhiteList(BotMessageAction messageAction) {
		Long senderId = messageAction.getBotSender().getId();
		Long mcSenderId = botConfigManager.getLongSenderConfigCache(senderId, "mcBind");
		Asserts.notNull(mcSenderId, "未绑定服务器");
		BotSender mcBotSender = botSenderCacheManager.getValidBotSenderById(mcSenderId);
		BotRobot mcBot = botRobotCacheManager.getValidBotRobotById(mcBotSender.getSendBot());
		Asserts.isTrue(BotRobotConstant.mcBotTypeList.contains(mcBot.getType()), "啊嘞，不对劲");

		Set<String> playerNameList = new HashSet<>();
		String value = messageAction.getValue();
		if (StringUtils.isNotBlank(value)) {
			playerNameList.addAll(Arrays.asList(value.split("[，,]")));
		}
		String body = messageAction.getBody();
		if (StringUtils.isNotBlank(body)) {
			playerNameList.addAll(Arrays.asList(body.split("\n")));
		}
		List<String> successList = new ArrayList<>();
		List<String> failList = new ArrayList<>();
		for (String playerName : playerNameList) {
			String result = botManager.execCommand(mcBot, "lp user " + playerName + " parent set player");
			if ("".equals(result)) {
				successList.add(playerName);
			} else {
				failList.add(playerName);
			}
		}

		return BotMessage.simpleTextMessage(String.format("执行完毕\n疑似成功：%s\n疑似失败：%s", successList, failList));
	}

	private BotMessage handelMcList(BotMessageAction messageAction) {
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
