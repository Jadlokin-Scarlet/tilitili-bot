package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotSessionService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.mcping.McPingMod;
import com.tilitili.common.entity.view.bot.mcping.McPingResponse;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class McPingHandle extends ExceptionRespMessageHandle {
	private static final String hostKey = "hostKey";

	private final McPingManager mcPingManager;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotForwardConfigMapper botForwardConfigMapper;
	private final BotRobotCacheManager botRobotCacheManager;
	private final BotConfigManager botConfigManager;
	private final BotManager botManager;
	private final BotRoleManager botRoleManager;

	@Autowired
	public McPingHandle(McPingManager mcPingManager, BotForwardConfigMapper botForwardConfigMapper, BotSenderCacheManager botSenderCacheManager, BotRobotCacheManager botRobotCacheManager, BotConfigManager botConfigManager, BotManager botManager, BotRoleManager botRoleManager) {
		this.mcPingManager = mcPingManager;
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
		boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
		if (!canUseBotAdminTask) {
			return null;
		}
		Long senderId = messageAction.getBotSender().getId();
		String bindSenderIdSrt = messageAction.getValue();
		Asserts.isTrue(botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser()), "权限不足");
		if (StringUtils.isBlank(bindSenderIdSrt)) {
			Long bindSenderId = botConfigManager.getLongSenderConfigCache(senderId, "mcBind");
			botConfigManager.deleteSenderConfig(senderId, "mcBind");
			return BotMessage.simpleTextMessage("解绑服务器"+bindSenderId);
		}
		Asserts.isNumber(bindSenderIdSrt, "格式错啦(服务器id)");
		Long bindSenderId = Long.valueOf(bindSenderIdSrt);
		BotSender bindSender = botSenderCacheManager.getValidBotSenderById(bindSenderId);
		Asserts.notNull(bindSender, "服务器不存在");
		Asserts.checkEquals(bindSender.getSendType(), SendTypeEnum.MINECRAFT_MESSAGE_STR, "服务器不存在");
		botConfigManager.addOrUpdateSenderConfig(senderId, "mcBind", bindSenderId);
		return BotMessage.simpleTextMessage("成功绑定服务器"+bindSender.getName());
	}

	private BotMessage handleWhiteList(BotMessageAction messageAction) {
		boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
		if (!canUseBotAdminTask) {
			return null;
		}
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
			String result = botManager.execCommand(mcBot, "lp user " + playerName + " parent set wanjia");
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

		// 先查绑定的转发消息渠道
		List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(senderId));
		if (!forwardConfigList.isEmpty()) {
			BotForwardConfig forwardConfig = forwardConfigList.get(0);
			Long targetSenderId = forwardConfig.getTargetSenderId();
			BotSender targetBotSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
			if (targetBotSender != null) {
				BotRobot minecraftBot = botRobotCacheManager.getValidBotRobotById(targetBotSender.getSendBot());

				List<BotUserDTO> userList = botManager.listOnlinePlayer(minecraftBot, targetBotSender);
				String playerListStr = userList.stream().map(BotUserDTO::getEnName).collect(Collectors.joining("，"));
				return BotMessage.simpleTextMessage("当前在线玩家：" + playerListStr);
			}
		}

		// 再查mc bind
		Long bindSenderId = botConfigManager.getLongSenderConfigCache(senderId, "mcBind");
		if (bindSenderId != null) {
			BotSender targetBotSender = botSenderCacheManager.getValidBotSenderById(bindSenderId);
			if (targetBotSender != null) {
				BotRobot minecraftBot = botRobotCacheManager.getValidBotRobotById(targetBotSender.getSendBot());

				List<BotUserDTO> userList = botManager.listOnlinePlayer(minecraftBot, targetBotSender);
				String playerListStr = userList.stream().map(BotUserDTO::getEnName).collect(Collectors.joining("，"));
				return BotMessage.simpleTextMessage("当前在线玩家："+playerListStr);
			}
		}

		return BotMessage.simpleTextMessage("未绑定服务器");
	}

	private BotMessage handleMcp(BotMessageAction messageAction) throws IOException {
		BotSessionService.MiraiSession session = messageAction.getSession();
		Long senderId = messageAction.getBotSender().getId();
		String key = messageAction.getKeyWithoutPrefix();
		Asserts.notBlank(key, "key是啥");
		if (key.equals("mcpd")) {
			String value = messageAction.getValue();
			Asserts.notBlank(value, "格式错啦(地址)");
			session.put(hostKey, value);
			return BotMessage.simpleTextMessage("记住啦！下次mcp不用加地址了。");
		}
		String url = messageAction.getValueOrDefault(session.get(hostKey));
		if (StringUtils.isBlank(url)) {
			// 先查绑定的转发消息渠道
			List<BotForwardConfig> forwardConfigList = botForwardConfigMapper.getBotForwardConfigByCondition(new BotForwardConfigQuery().setSourceSenderId(senderId));
			if (!forwardConfigList.isEmpty()) {
				BotForwardConfig forwardConfig = forwardConfigList.get(0);
				Long targetSenderId = forwardConfig.getTargetSenderId();
				BotSender targetBotSender = botSenderCacheManager.getValidBotSenderById(targetSenderId);
				BotRobot minecraftBot = botRobotCacheManager.getValidBotRobotById(targetBotSender.getSendBot());
				url = minecraftBot.getHost();
			}

			// 再查mc bind
			Long bindSenderId = botConfigManager.getLongSenderConfigCache(senderId, "mcBind");
			if (bindSenderId != null) {
				BotSender targetBotSender = botSenderCacheManager.getValidBotSenderById(bindSenderId);
				if (targetBotSender != null) {
					BotRobot minecraftBot = botRobotCacheManager.getValidBotRobotById(targetBotSender.getSendBot());
					url = minecraftBot.getHost();
				}
			}
		}
		Asserts.notBlank(url, "没有地址，请发送（帮助 mcp）查看详情");
		URL urlFormat = new URL(url);
		String host = urlFormat.getHost();
		int port = urlFormat.getPort() != -1 ? urlFormat.getPort() : 25565;
		McPingResponse response;
		try {
			response = mcPingManager.mcPing(new InetSocketAddress(host, port));
		} catch (IOException e) {
			try {
				BotMessage resp = this.handelMcList(messageAction);
				List<BotMessageChain> respList = resp.getBotMessageChainList();
				respList.add(0, BotMessageChain.ofPlain("mc ping失败，一下是在线玩家查询结果：\n"));
				return BotMessage.simpleListMessage(respList);
			} catch (Exception ee) {
				throw new AssertException("网络异常", e);
			}
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
