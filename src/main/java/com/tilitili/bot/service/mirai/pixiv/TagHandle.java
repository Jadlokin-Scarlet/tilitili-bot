package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TagHandle extends ExceptionRespMessageHandle {
	private final PixivCacheManager pixivManager;
	private final PixivCacheService pixivService;
	private final BotMessageService botMessageService;

	@Autowired
	public TagHandle(PixivCacheManager pixivManager, PixivCacheService pixivService, BotMessageService botMessageService) {
		this.pixivManager = pixivManager;
		this.pixivService = pixivService;
		this.botMessageService = botMessageService;
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
		String tagList = info.getTags().getTags().stream().map(PixivInfoTag::getTag).collect(Collectors.joining("    "));
		Asserts.notBlank(tagList, "没找到图片");
		return BotMessage.simpleTextMessage(String.format("[%s]的tag有：%s", pid, tagList));
	}
}
