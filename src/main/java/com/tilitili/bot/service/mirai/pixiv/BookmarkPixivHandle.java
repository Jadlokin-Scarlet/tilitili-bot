package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotUser;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class BookmarkPixivHandle extends ExceptionRespMessageHandle {
	private final BotMessageService botMessageService;
	private final PixivCacheService pixivService;
	private final PixivCacheManager pixivManager;
	private final PixivLoginUserMapper pixivLoginUserMapper;
	private final RedisCache redisCache;

	@Autowired
	public BookmarkPixivHandle(BotMessageService botMessageService, PixivCacheService pixivService, PixivCacheManager pixivManager, PixivLoginUserMapper pixivLoginUserMapper, RedisCache redisCache) {
		this.botMessageService = botMessageService;
		this.pixivService = pixivService;
		this.pixivManager = pixivManager;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotMessage botMessage = messageAction.getBotMessage();
		BotUser botUser = messageAction.getBotUser();
		Long userId = botUser.getId();

		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		if (StringUtils.isBlank(pid)) {
			pid = botMessageService.getQuotePid(messageAction);
		}
		if (StringUtils.isBlank(pid)) {
			String url = botMessageService.getFirstImageListOrQuoteImage(messageAction);
			pid = pixivService.findPixivImage(url);
		}
		Asserts.isNumber(pid, "格式错啦(pid)");

		PixivInfoIllust infoProxy = pixivManager.getInfoProxy(pid);
		Asserts.notNull(infoProxy, "啊嘞，不对劲");

		PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserByUserId(userId);
		Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
		String cookie = pixivLoginUser.getCookie();
		Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

		String token = pixivManager.getPixivToken(userId, cookie);
		pixivManager.bookmarkImageForCookie(pid, cookie, token);

		for (String mode : Arrays.asList("all", "safe", "r18")) {
			redisCache.delete(PixivRecommendHandle.pixivImageListKey + userId + mode);
			redisCache.delete(PixivRecommendHandle.pixivImageListPageNoKey + userId + mode);
		}
		return BotMessage.simpleTextMessage("搞定！");
	}
}
