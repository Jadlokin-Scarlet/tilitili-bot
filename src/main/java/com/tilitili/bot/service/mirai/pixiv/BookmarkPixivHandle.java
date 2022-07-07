package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookmarkPixivHandle extends ExceptionRespMessageHandle {
	private final BotMessageService botMessageService;
	private final PixivService pixivService;
	private final PixivManager pixivManager;
	private final PixivLoginUserMapper pixivLoginUserMapper;
	private final RedisCache redisCache;

	@Autowired
	public BookmarkPixivHandle(BotMessageService botMessageService, PixivService pixivService, PixivManager pixivManager, PixivLoginUserMapper pixivLoginUserMapper, RedisCache redisCache) {
		this.botMessageService = botMessageService;
		this.pixivService = pixivService;
		this.pixivManager = pixivManager;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotMessage botMessage = messageAction.getBotMessage();
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

		Long qq = botMessage.getQq();
		Long tinyId = botMessage.getTinyId();
		Long sender = qq != null? qq : tinyId;
		Asserts.notNull(sender, "发送者为空");

		PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserBySender(sender);
		Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
		String cookie = pixivLoginUser.getCookie();
		Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

		String token = pixivManager.getPixivToken(sender, cookie);
		pixivManager.bookmarkImageForCookie(pid, cookie, token);

		redisCache.delete(PixivRecommendHandle.pixivImageListKey + sender);
		redisCache.delete(PixivRecommendHandle.pixivImageListPageNoKey + sender);
		return BotMessage.simpleTextMessage("搞定！");
	}
}
