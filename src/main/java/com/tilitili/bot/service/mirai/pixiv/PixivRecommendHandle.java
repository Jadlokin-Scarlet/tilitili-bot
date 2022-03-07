package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivRecommendIllust;
import com.tilitili.common.manager.BotSenderManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
//@Component
public class PixivRecommendHandle extends ExceptionRespMessageHandle {
	private final String pixivImageKey = "pixivImageKey-";
	private final RedisCache redisCache;
	private final PixivLoginUserMapper pixivLoginUserMapper;
	private final BotSenderManager botSenderManager;
	private final PixivManager pixivManager;

	@Autowired
	public PixivRecommendHandle(RedisCache redisCache, PixivLoginUserMapper pixivLoginUserMapper, BotSenderManager botSenderManager, PixivManager pixivManager) {
		this.redisCache = redisCache;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
		this.botSenderManager = botSenderManager;
		this.pixivManager = pixivManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotMessage botMessage = messageAction.getBotMessage();

		Long qq = botMessage.getQq();
		String tinyId = botMessage.getTinyId();
		String sender = qq != null? String.valueOf(qq) : tinyId;
		Asserts.notBlank(sender, "发送者为空");
		PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserBySender(sender);
		Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
		String cookie = pixivLoginUser.getCookie();
		Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

		String key = messageAction.getKeyWithoutPrefix();
		if (Objects.equals(key, "好")) {
			String pid = (String) redisCache.getValue(pixivImageKey + sender);

		}

		PixivRecommendIllust illust = pixivManager.getRecommendImageByCookie(cookie);
		String pid = illust.getId();

		return null;
	}
}
