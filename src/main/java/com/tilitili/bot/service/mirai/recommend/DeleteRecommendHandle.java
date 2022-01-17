package com.tilitili.bot.service.mirai.recommend;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.Recommend;
import com.tilitili.common.entity.RecommendVideo;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.RecommendMapper;
import com.tilitili.common.mapper.tilitili.RecommendVideoMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.BilibiliUtil;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteRecommendHandle extends ExceptionRespMessageHandle {
	private final RecommendMapper recommendMapper;
	private final RecommendVideoMapper recommendVideoMapper;

	@Autowired
	public DeleteRecommendHandle(RecommendMapper recommendMapper, RecommendVideoMapper recommendVideoMapper) {
		this.recommendMapper = recommendMapper;
		this.recommendVideoMapper = recommendVideoMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String avOrBv = messageAction.getValue();

		RecommendVideo newVideo = recommendVideoMapper.getNew();

		long av;
		if (StringUtils.isNumber(avOrBv)) {
			av = Long.parseLong(avOrBv);
		} else {
			av = BilibiliUtil.converseBvToAv(avOrBv);
		}

		Recommend oldRecommend = recommendMapper.getByAv(av);
		Asserts.notNull(oldRecommend, "没有找到该视频。");
		Asserts.notEquals(oldRecommend.getStatus(), -1, "没有找到该视频。");
		if (oldRecommend.getStatus().equals(1)) {
			Asserts.checkEquals(oldRecommend.getIssueId(), newVideo.getId(), "该视频在往期中，不能删。");
		}

		recommendMapper.update(new Recommend().setId(oldRecommend.getId()).setStatus(-1));
		return BotMessage.simpleTextMessage("已删除。");
	}
}
