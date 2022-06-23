package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.mysql.PixivTagMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TagHandle extends ExceptionRespMessageHandle {
	private final PixivTagMapper pixivTagMapper;
	private final PixivService pixivService;
	private final BotMessageService botMessageService;

	@Autowired
	public TagHandle(PixivTagMapper pixivTagMapper, PixivService pixivService, BotMessageService botMessageService) {
		this.pixivTagMapper = pixivTagMapper;
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
		Asserts.notBlank(pid, "格式错啦(pid)");
		List<PixivTag> tagList = pixivTagMapper.getPixivTagByCondition(new PixivTagQuery().setPid(pid));
		if (tagList.isEmpty()) {
			pixivService.saveImageFromPixiv(pid);
			tagList = pixivTagMapper.getPixivTagByCondition(new PixivTagQuery().setPid(pid));
		}
		Asserts.notEmpty(tagList, "没找到图片");
		return BotMessage.simpleTextMessage(String.format("[%s]的tag有：%s", pid, tagList.stream().map(PixivTag::getTag).collect(Collectors.joining("    "))));
	}
}
