package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivRecommendIllust;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class PixivRecommendHandle extends ExceptionRespMessageHandle {
	private final String pixivImageKey = "pixivImageKey-";
	private final RedisCache redisCache;
	private final PixivManager pixivManager;
	private final PixivLoginUserMapper pixivLoginUserMapper;

	@Autowired
	public PixivRecommendHandle(RedisCache redisCache, PixivLoginUserMapper pixivLoginUserMapper, PixivManager pixivManager) {
		this.redisCache = redisCache;
		this.pixivManager = pixivManager;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
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
			if (pid != null) {
				pixivManager.bookmarkImageForCookie(pid, cookie);
			} else {
				return null;
			}
		}

		PixivRecommendIllust illust = pixivManager.getRecommendImageByCookie(cookie);
		String pid = illust.getId();
		Integer sl = illust.getSl();
		List<String> urlList = pixivManager.getPageListProxy(pid);

		if (urlList.size() > 10) {
			urlList = urlList.subList(0, 10);
		}

		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(new BotMessageChain().setType("Plain").setText("https://pixiv.moe/illust/"+pid));
		for (String url : urlList) {
			String ossUrl = OSSUtil.uploadSOSSByUrl(url);
			messageChainList.add(new BotMessageChain().setType("Plain").setText("\n"));
			if (sl == null || sl < 5) {
				Asserts.notNull(ossUrl, "上传OSS失败");
				messageChainList.add(new BotMessageChain().setType("Image").setUrl(ossUrl));
			} else {
				messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl != null ? ossUrl : url));
			}
		}

		redisCache.setValue(pixivImageKey + sender, pid);
		return BotMessage.simpleListMessage(messageChainList, botMessage);
	}
}
