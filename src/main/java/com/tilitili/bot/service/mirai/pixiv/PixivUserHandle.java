package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PixivUserHandle extends ExceptionRespMessageHandle {
	private final PixivImageMapper pixivImageMapper;
	private final PixivService pixivService;
	private final BotMessageService botMessageService;

	@Autowired
	public PixivUserHandle(PixivImageMapper pixivImageMapper, PixivService pixivService, BotMessageService botMessageService) {
		this.pixivImageMapper = pixivImageMapper;
		this.pixivService = pixivService;
		this.botMessageService = botMessageService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValueOrDefault(pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction))));
		Asserts.notBlank(pid, "格式错啦(pid)");
		List<PixivImage> imageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setSource("pixiv").setPid(pid));
		if (imageList.isEmpty()) {
			pixivService.saveImageFromPixiv(pid);
			imageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setSource("pixiv").setPid(pid));
		}
		Asserts.checkEquals(imageList.size(), 1, "没找到。");
		PixivImage pixivImage = imageList.get(0);

		return BotMessage.simpleTextMessage(String.format("[%s]的作者是 %s", pid, pixivImage.getUserName()));
	}
}
