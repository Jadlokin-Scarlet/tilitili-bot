package com.tilitili.bot.service.mirai.talk;

import com.tilitili.bot.entity.ExcelResult;
import com.tilitili.bot.entity.RandomTalkDTO;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.BaseMessageHandleAdapt;
import com.tilitili.bot.util.ExcelUtil;
import com.tilitili.common.entity.query.BotTalkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.mapper.mysql.BotTalkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StreamUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

//@Component
public class AddRandomTalkHandle extends BaseMessageHandleAdapt {
	private final BotTalkMapper botTalkMapper;
	private final BotManager botManager;

	@Autowired
	public AddRandomTalkHandle(BotTalkMapper botTalkMapper, BotManager botManager) {
		this.botTalkMapper = botTalkMapper;
		this.botManager = botManager;
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
		Asserts.isTrue(fileName.contains("-"), "文件名不对哦。");
		String function = fileName.split("-")[1];

		String fileId = fileChain.getId();
		Asserts.notNull(fileId, "啊嘞，找不到文件。");
		File file = botManager.downloadGroupFile(messageAction.getBot(), messageAction.getBotMessage().getGroup(), fileId);
		ExcelResult<RandomTalkDTO> excelResult = ExcelUtil.getListFromExcel(file, RandomTalkDTO.class);
		List<RandomTalkDTO> resultList = excelResult.getResultList();

		botTalkMapper.getBotTalkByCondition(new BotTalkQuery());

		return null;
	}
}
