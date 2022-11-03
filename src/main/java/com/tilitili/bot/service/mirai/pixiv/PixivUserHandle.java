package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PixivUserHandle extends ExceptionRespMessageHandle {
	private final PixivCacheService pixivService;
	private final BotMessageService botMessageService;
	private final PixivCacheManager pixivManager;

	@Autowired
	public PixivUserHandle(PixivCacheService pixivService, BotMessageService botMessageService, PixivCacheManager pixivManager) {
		this.pixivService = pixivService;
		this.botMessageService = botMessageService;
		this.pixivManager = pixivManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		if (StringUtils.isBlank(pid)) {
			pid = botMessageService.getQuotePid(messageAction);
		}
		if (StringUtils.isBlank(pid)) {
			pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
		}
		Asserts.isNumber(pid, "格式错啦(pid)");

		PixivInfoIllust info = pixivManager.getInfoProxy(pid);
		String userName = info.getUserName();

		return BotMessage.simpleTextMessage(String.format("[%s]的作者是 %s", pid, userName));
	}
}
