package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivRecommendIllust;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class PixivRecommendHandle extends ExceptionRespMessageHandle {
	private final static String pixivImageKey = "pixivImageKey-";
	public final static String pixivImageListKey = "pixivImageListKey-";
	public final static String pixivImageListPageNoKey = "pixivImageListPageNoKey-";
	private final RedisCache redisCache;
	private final PixivManager pixivManager;
	private final PixivLoginUserMapper pixivLoginUserMapper;
	private final BotTaskMapper botTaskMapper;

	@Autowired
	public PixivRecommendHandle(RedisCache redisCache, PixivLoginUserMapper pixivLoginUserMapper, PixivManager pixivManager, BotTaskMapper botTaskMapper) {
		this.redisCache = redisCache;
		this.pixivManager = pixivManager;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
		this.botTaskMapper = botTaskMapper;
	}

	private final Map<String, String> keyModeMap = ImmutableMap.of("推荐色图", "all", "推荐色色", "r18", "推荐不色", "safe");

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotMessage botMessage = messageAction.getBotMessage();
		log.debug("PixivRecommendHandle start");

		String key = messageAction.getKeyWithoutPrefix();
		boolean isBookmark = Objects.equals(key, "好");
		boolean isNoBookmark = Objects.equals(key, "不");
		String mode = keyModeMap.getOrDefault(key, "all");
		log.debug("PixivRecommendHandle get cookie");
		Long qq = botMessage.getQq();
		String tinyId = botMessage.getTinyId();
		String sender = qq != null? String.valueOf(qq) : tinyId;
		Asserts.notBlank(sender, "发送者为空");

		BotSender botSender = messageAction.getBotSender();
		List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
		boolean canSS = !botTaskList.isEmpty();

		if (!canSS) {
			Asserts.notEquals(mode, "r18", "不准色色");
		}

		PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserBySender(sender);
		if ((isBookmark || isNoBookmark) && !redisCache.exists(pixivImageKey + sender)) {
			log.info("无可收藏的pid");
			return null;
		}
		Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
		String cookie = pixivLoginUser.getCookie();
		Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

		log.debug("PixivRecommendHandle bookmark");
		if (isBookmark) {
			String pidAndMode = (String) redisCache.getValue(pixivImageKey + sender);
			String pid = pidAndMode.split("_")[0];
			mode = pidAndMode.split("_")[1];
			if (pid != null) {
				String token = pixivManager.getPixivToken(sender, cookie);
				pixivManager.bookmarkImageForCookie(pid, cookie, token);
			} else {
				return null;
			}
			redisCache.delete(pixivImageListKey+sender + mode);
			redisCache.delete(pixivImageListPageNoKey + sender + mode);
		}
		if (isNoBookmark) {
			String pidAndMode = (String) redisCache.getValue(pixivImageKey + sender);
			mode = pidAndMode.split("_")[1];
		}

		log.debug("PixivRecommendHandle get info");
		PixivRecommendIllust illust;
		while (true) {
			int pageNo = Math.toIntExact(redisCache.increment(pixivImageListPageNoKey + sender + mode)) - 1;
			if (pageNo >= 60) {
				redisCache.delete(pixivImageListKey + sender + mode);
				redisCache.delete(pixivImageListPageNoKey + sender + mode);
				pageNo = Math.toIntExact(redisCache.increment(pixivImageListPageNoKey + sender + mode)) - 1;
			}
			List<PixivRecommendIllust> illustList;
			if (redisCache.exists(pixivImageListKey + sender + mode)) {
				illustList = (List<PixivRecommendIllust>) redisCache.getValue(pixivImageListKey + sender + mode);
			} else {
				illustList = pixivManager.getRecommendImageByCookie(cookie, mode);
				redisCache.setValue(pixivImageListKey + sender + mode, illustList);
			}
			if (illustList == null || pageNo >= illustList.size()) {
				return BotMessage.simpleTextMessage("啊嘞，怎么会找不到呢。");
			}
			illust = illustList.get(pageNo);
			if (!canSS && illust.getSl() > 4) {
				continue;
			}
			break;
		}

		String pid = illust.getId();
		Integer sl = illust.getSl();
		log.debug("PixivRecommendHandle get url");
		List<String> urlList = pixivManager.getPageListProxy(pid);

		if (urlList.size() > 10) {
			urlList = urlList.subList(0, 10);
		}

		log.debug("PixivRecommendHandle get make messageChainList");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("https://pixiv.moe/illust/"+pid));
		for (String url : urlList) {
			String ossUrl = OSSUtil.uploadSOSSByUrl(url);
			messageChainList.add(BotMessageChain.ofPlain("\n"));
			if (sl == null || sl < 5) {
				Asserts.notNull(ossUrl, "上传OSS失败");
				messageChainList.add(BotMessageChain.ofImage(ossUrl));
			} else {
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null ? ossUrl : url));
			}
		}

		log.debug("PixivRecommendHandle save result");
		redisCache.setValue(pixivImageKey + sender, pid + "_" + mode, 120);
		log.debug("PixivRecommendHandle send");
		return BotMessage.simpleListMessage(messageChainList, botMessage);
	}
}