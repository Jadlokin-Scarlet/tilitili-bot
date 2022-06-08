package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.emnus.TaskReason;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.message.SimpleTask;
import com.tilitili.common.manager.TaskManager;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class FindSeseBookHandle extends ExceptionRespMessageHandle {
	private final TaskManager taskManager;

	public FindSeseBookHandle(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		List<String> imageList = messageAction.getImageList();
		String messageId = messageAction.getMessageId();
		Asserts.notBlank(messageId, "参数异常");
		Asserts.notEmpty(imageList, "格式错啦(图片)");
		String url = imageList.get(0);
		Asserts.notBlank(url, "格式错啦(图片)");

		return BotMessage.simpleTextMessage("还不支持喵。用这个吧[https://soutubot.moe/]");
//		taskManager.simpleSpiderVideo(new SimpleTask().setReason(TaskReason.SPIDER_SESE_BOOK.value).setValueList(Arrays.asList(url, messageId)));
//		return BotMessage.emptyMessage();
	}
}
