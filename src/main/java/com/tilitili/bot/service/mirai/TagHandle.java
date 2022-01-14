package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.PixivTagMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TagHandle extends ExceptionRespMessageHandle {
	private final PixivTagMapper pixivTagMapper;

	@Autowired
	public TagHandle(PixivTagMapper pixivTagMapper) {
		this.pixivTagMapper = pixivTagMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		Asserts.notBlank(pid, "格式错啦(pid)");
		List<PixivTag> tagList = pixivTagMapper.getPixivTagByCondition(new PixivTagQuery().setPid(pid));
		return BotMessage.simpleTextMessage(String.format("[%s]的tag有：%s", pid, tagList.stream().map(PixivTag::getTag).collect(Collectors.joining("    "))));
	}
}
