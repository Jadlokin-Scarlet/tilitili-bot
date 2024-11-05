package com.tilitili.bot.service.mirai;

import com.alibaba.fastjson2.JSONObject;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSenderTaskMapping;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.BotRoleManager;
import com.tilitili.common.mapper.mysql.BotSenderTaskMappingMapper;
import com.tilitili.common.mapper.mysql.automapper.BotTaskAutoMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class NailongRemoveHandle extends ExceptionRespMessageHandle {
	private final BotManager botManager;
	private final BotRoleManager botRoleManager;
	private final BotTaskAutoMapper botTaskMapper;
	private final BotSenderTaskMappingMapper botSenderTaskMappingMapper;

	public NailongRemoveHandle(BotManager botManager, BotRoleManager botRoleManager, BotTaskAutoMapper botTaskMapper, BotSenderTaskMappingMapper botSenderTaskMappingMapper) {
		this.botManager = botManager;
		this.botRoleManager = botRoleManager;
		this.botTaskMapper = botTaskMapper;
		this.botSenderTaskMappingMapper = botSenderTaskMappingMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "TD": return this.handleTD(messageAction);
			default: return this.handleRemove(messageAction);
		}
	}

	private BotMessage handleTD(BotMessageAction messageAction) {
		boolean canUseBotAdminTask = botRoleManager.canUseBotAdminTask(messageAction.getBot(), messageAction.getBotUser());
		if (!canUseBotAdminTask) {
			return null;
		}
		if (!messageAction.getSession().containsKey("NailongRemoveHandle.TDKey")) {
			return null;
		}

		Long senderId = messageAction.getBotSender().getId();
		BotTask task = botTaskMapper.getBotTaskByNick("撤回奶龙");
		Asserts.notNull(task);

		BotSenderTaskMapping botSenderTaskMapping = botSenderTaskMappingMapper.getBotSenderTaskMappingBySenderIdAndTaskId(senderId, task.getId());
		if (botSenderTaskMapping != null) {
			botSenderTaskMappingMapper.deleteBotSenderTaskMappingById(botSenderTaskMapping.getId());
		}
		return BotMessage.simpleTextMessage("√");
	}

	private BotMessage handleRemove(BotMessageAction messageAction) {
		List<String> imageList = messageAction.getImageList();
		for (String imageUrl : imageList) {
			imageUrl = imageUrl.replaceFirst("https", "http");
			String result = HttpClientUtil.httpPost("http://172.27.0.7:8081/check_image?image_url=" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8));
			log.info("check image url:{} result:{}", imageUrl, result);
			Asserts.notBlank(result, "网络异常");
			Boolean checkOk = JSONObject.parseObject(result).getBoolean("check_ok");
			log.info("check ok:{}", checkOk);
			if (checkOk) {
				botManager.recallMessage(messageAction.getBot(), messageAction.getBotSender(), messageAction.getMessageId());
				messageAction.getSession().put("NailongRemoveHandle.TDKey", "yes", 60*10);
				return BotMessage.simpleTextMessage("已撤回奶龙，管理员回复TD退订。");
			}
		}
		return null;
	}
}
