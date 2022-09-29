package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.RandomTalkDTO;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.bot.util.ExcelUtil;
import com.tilitili.common.entity.BotFunction;
import com.tilitili.common.entity.BotFunctionTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.GoCqhttpManager;
import com.tilitili.common.mapper.mysql.BotFunctionMapper;
import com.tilitili.common.mapper.mysql.BotFunctionTalkMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AddRandomTalkHandle extends BaseMessageHandleAdapt {
	private final BotManager botManager;
	private final BotSenderMapper botSenderMapper;
	private final GoCqhttpManager goCqhttpManager;
	private final BotFunctionMapper botFunctionMapper;
	private final BotFunctionTalkMapper botFunctionTalkMapper;

	@Autowired
	public AddRandomTalkHandle(BotManager botManager, BotSenderMapper botSenderMapper, GoCqhttpManager goCqhttpManager, BotFunctionMapper botFunctionMapper, BotFunctionTalkMapper BotFunctionTalkMapper) {
		this.botManager = botManager;
		this.botSenderMapper = botSenderMapper;
		this.goCqhttpManager = goCqhttpManager;
		this.botFunctionMapper = botFunctionMapper;
		this.botFunctionTalkMapper = BotFunctionTalkMapper;
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
		String groupList = excelResult.getParam("群号");


		BotFunction newFunction = new BotFunction().setFunction(function);
		botFunctionMapper.addBotFunctionSelective(newFunction);
		List<BotSender> botSenderList = Arrays.stream(groupList.split(",")).map(Long::valueOf)
				.map(botSenderMapper::getBotSenderByGroup)
				.filter(Objects::nonNull).collect(Collectors.toList());
		List<BotFunctionTalk> newFunctionTalkList = new ArrayList<>();
		for (RandomTalkDTO randomTalkDTO : resultList) {
			String req = TalkHandle.convertMessageToString(BotMessage.simpleListMessage(goCqhttpManager.convertCqToMessageChain(randomTalkDTO.getReq())));
			String resp = TalkHandle.convertMessageToString(BotMessage.simpleListMessage(goCqhttpManager.convertCqToMessageChain(randomTalkDTO.getResp())));
			for (BotSender botSender : botSenderList) {
				BotFunctionTalk newFunctionTalk = new BotFunctionTalk().setReq(req).setResp(resp).setFunction(function).setFunctionId(newFunction.getId()).setSenderId(botSender.getId());
				newFunctionTalkList.add(newFunctionTalk);
			}
		}

		BotFunction oldFunction = botFunctionMapper.getLastFunction(function);
		List<BotFunctionTalk> oldFunctionTalkList = botFunctionTalkMapper.getBotFunctionTalkByCondition(new BotFunctionTalkQuery().setFunctionId(oldFunction.getId()));

		for (BotFunctionTalk oldFunctionTalk : oldFunctionTalkList) {
			botFunctionTalkMapper.updateBotFunctionTalkSelective(new BotFunctionTalk().setId(oldFunctionTalk.getId()).setStatus(-1));
		}
		for (BotFunctionTalk newFunctionTalk : newFunctionTalkList) {
			botFunctionTalkMapper.addBotFunctionTalkSelective(newFunctionTalk);
		}

		return BotMessage.simpleTextMessage("搞定√");
	}
}
