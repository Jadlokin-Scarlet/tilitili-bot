package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.RandomTalkDTO;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.FunctionTalkService;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.bot.util.ExcelUtil;
import com.tilitili.common.entity.BotFunction;
import com.tilitili.common.entity.BotFunctionTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotFunctionMapper;
import com.tilitili.common.mapper.mysql.BotFunctionTalkMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AddRandomTalkHandle extends BaseMessageHandleAdapt {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;
	private final BotFunctionMapper botFunctionMapper;
	private final BotFunctionTalkMapper botFunctionTalkMapper;
	private final FunctionTalkService functionTalkService;

	@Autowired
	public AddRandomTalkHandle(BotManager botManager, BotSenderMapper botSenderMapper, BotFunctionMapper botFunctionMapper, BotFunctionTalkMapper BotFunctionTalkMapper, FunctionTalkService functionTalkService) {
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
		this.botFunctionMapper = botFunctionMapper;
		this.botFunctionTalkMapper = BotFunctionTalkMapper;
		this.functionTalkService = functionTalkService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		List<BotMessageChain> chainList = messageAction.getBotMessage().getBotMessageChainList();
		BotMessageChain fileChain = chainList.stream().filter(StreamUtil.isEqual(BotMessageChain::getType, BotMessage.MESSAGE_TYPE_FILE)).findFirst().orElse(null);
		if (fileChain == null) {
			return null;
		}
		String fileName = fileChain.getName();
		Asserts.isTrue(fileName.startsWith("随机对话模板"), "文件名不对哦。");

		String fileId = fileChain.getId();
		Asserts.notNull(fileId, "啊嘞，找不到文件。");
		File file = botManager.downloadGroupFile(messageAction.getBot(), messageAction.getBotMessage().getGroup(), fileId);
		ExcelResult<RandomTalkDTO> excelResult = ExcelUtil.getListFromExcel(file, RandomTalkDTO.class);
		List<RandomTalkDTO> resultList = excelResult.getResultList();
		String function = excelResult.getParam("分组");
		String friendList = excelResult.getParam("私聊");
		String groupList = excelResult.getParam("群号");
		String guildList = excelResult.getParam("频道");
		String channelList = excelResult.getParam("子频道");
		String scoreStr = excelResult.getParamOrDefault("积分", "0");
		Asserts.notBlank(function, "分组不能为空");
		Asserts.isNumber(scoreStr, "积分请输入正整数");
		int score = Integer.parseInt(scoreStr);

		BotFunction oldFunction = botFunctionMapper.getLastFunction(function);
		BotFunction newFunction = new BotFunction().setFunction(function).setScore(score);
		botFunctionMapper.addBotFunctionSelective(newFunction);
		List<Long> botSenderIdList = new ArrayList<>();
		if (StringUtils.isNotBlank(friendList)) {
			botSenderIdList.addAll(Arrays.stream(friendList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByQq)
					.filter(Objects::nonNull).map(BotSender::getId).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(groupList)) {
			botSenderIdList.addAll(Arrays.stream(groupList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByGroup)
					.filter(Objects::nonNull).map(BotSender::getId).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(guildList)) {
			botSenderIdList.addAll(Arrays.stream(guildList.split(",")).map(Long::valueOf)
					.flatMap(guildId -> botSenderMapper.getBotSenderByCondition(new BotSenderQuery().setGuildId(guildId).setStatus(0)).stream())
					.filter(Objects::nonNull).map(BotSender::getId).collect(Collectors.toList()));
		}
		if (StringUtils.isNotBlank(channelList)) {
			botSenderIdList.addAll(Arrays.stream(channelList.split(",")).map(Long::valueOf)
					.map(botSenderMapper::getBotSenderByChannelId)
					.filter(Objects::nonNull).map(BotSender::getId).collect(Collectors.toList()));
		}
		List<BotFunctionTalk> newFunctionTalkList = new ArrayList<>();
		for (RandomTalkDTO randomTalkDTO : resultList) {
			Asserts.notBlank(randomTalkDTO.getReq(), "关键词不能为空");
			Asserts.notBlank(randomTalkDTO.getResp(), "回复不能为空");
			String req = Gsons.toJson(BotMessage.simpleListMessage(functionTalkService.convertCqToMessageChain(randomTalkDTO.getReq())));
			String resp = Gsons.toJson(BotMessage.simpleListMessage(functionTalkService.convertCqToMessageChain(randomTalkDTO.getResp())));
			for (Long botSenderId : botSenderIdList) {
				BotFunctionTalk newFunctionTalk = new BotFunctionTalk().setReq(req).setResp(resp).setFunction(function).setFunctionId(newFunction.getId()).setSenderId(botSenderId).setBlackList(randomTalkDTO.getBlackList());
				newFunctionTalkList.add(newFunctionTalk);
			}
		}
		if (oldFunction != null) {
			List<BotFunctionTalk> oldFunctionTalkList = botFunctionTalkMapper.getBotFunctionTalkByCondition(new BotFunctionTalkQuery().setFunctionId(oldFunction.getId()));

			for (BotFunctionTalk oldFunctionTalk : oldFunctionTalkList) {
				botFunctionTalkMapper.updateBotFunctionTalkSelective(new BotFunctionTalk().setId(oldFunctionTalk.getId()).setStatus(-1));
			}
		}
		for (BotFunctionTalk newFunctionTalk : newFunctionTalkList) {
			botFunctionTalkMapper.addBotFunctionTalkSelective(newFunctionTalk);
		}

		String senderMessage = (StringUtils.isNotBlank(friendList)? "私聊"+friendList: "") +
				(StringUtils.isNotBlank(groupList)? "群号"+groupList: "") +
				(StringUtils.isNotBlank(guildList)? "频道"+guildList: "") +
				(StringUtils.isNotBlank(channelList)? "子频道"+channelList: "");
		String scoreMessage = score == 0? "": String.format("，积分%s", score);
		return BotMessage.simpleTextMessage(String.format("搞定√(分组%s导入%s条对话，%s%s)", function, newFunctionTalkList.size(), senderMessage, scoreMessage));
	}
}
