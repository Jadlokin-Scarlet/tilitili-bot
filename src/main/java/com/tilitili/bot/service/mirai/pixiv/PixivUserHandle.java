package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PixivUserHandle extends ExceptionRespMessageHandle {
	private final PixivImageMapper pixivImageMapper;

	@Autowired
	public PixivUserHandle(PixivImageMapper pixivImageMapper) {
		this.pixivImageMapper = pixivImageMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		Asserts.notBlank(pid, "格式错啦(pid)");
		List<PixivImage> imageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setSource("pixiv").setPid(pid));
		Asserts.checkEquals(imageList.size(), 1, "没找到。");
		PixivImage pixivImage = imageList.get(0);
		return BotMessage.simpleTextMessage(String.format("[%s]的作者是 %s", pid, pixivImage.getUserName()));
	}
}
