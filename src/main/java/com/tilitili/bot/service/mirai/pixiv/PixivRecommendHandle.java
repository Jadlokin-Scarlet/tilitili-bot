package com.tilitili.bot.service.mirai.pixiv;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.PixivLoginUser;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivRecommendIllust;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.PixivLoginUserMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	private final PixivCacheManager pixivManager;
	private final PixivLoginUserMapper pixivLoginUserMapper;
	private final BotTaskMapper botTaskMapper;
	private final PixivCacheService pixivService;

	@Autowired
	public PixivRecommendHandle(RedisCache redisCache, PixivLoginUserMapper pixivLoginUserMapper, PixivCacheManager pixivManager, BotTaskMapper botTaskMapper, PixivCacheService pixivService) {
		this.redisCache = redisCache;
		this.pixivManager = pixivManager;
		this.pixivLoginUserMapper = pixivLoginUserMapper;
		this.botTaskMapper = botTaskMapper;
		this.pixivService = pixivService;
	}

	private final Map<String, String> keyModeMap = ImmutableMap.of("推荐色图", "all", "推荐色色", "r18", "推荐不色", "safe", "tjst", "all", "tjss", "r18", "tjbs", "safe");

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotMessage botMessage = messageAction.getBotMessage();
		BotUserDTO botUser = messageAction.getBotUser();
		Long userId = botUser.getId();
		log.debug("PixivRecommendHandle start");

		String key = messageAction.getKeyWithoutPrefix();
		boolean isBookmark = Objects.equals(key, "好");
		boolean isNoBookmark = Objects.equals(key, "不");
		String mode = keyModeMap.getOrDefault(key, "all");
		log.debug("PixivRecommendHandle get cookie");


		BotSender botSender = messageAction.getBotSender();
		List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
		boolean canSS = !botTaskList.isEmpty();

		if (!canSS) {
			Asserts.notEquals(mode, "r18", "不准色色");
		}

		String pixivImageRedisKey = pixivImageKey + userId;

		PixivLoginUser pixivLoginUser = pixivLoginUserMapper.getPixivLoginUserByUserId(userId);
		if ((isBookmark || isNoBookmark) && !redisCache.exists(pixivImageRedisKey)) {
			log.info("无可收藏的pid");
			return null;
		}
		Asserts.notNull(pixivLoginUser, "先私聊绑定pixiv账号吧。");
		String cookie = pixivLoginUser.getCookie();
		Asserts.notBlank(cookie, "先私聊绑定pixiv账号吧。");

		log.debug("PixivRecommendHandle bookmark");
		if (isBookmark) {
			String pidAndMode = (String) redisCache.getValue(pixivImageRedisKey);
			String pid = pidAndMode.split("_")[0];
			mode = pidAndMode.split("_")[1];
			if (pid != null) {
				String token = pixivManager.getPixivToken(userId, cookie);
				pixivManager.bookmarkImageForCookie(pid, cookie, token);
			} else {
				return null;
			}
			redisCache.delete(pixivImageListKey + userId + mode);
			redisCache.delete(pixivImageListPageNoKey + userId + mode);
		}
		if (isNoBookmark) {
			String pidAndMode = (String) redisCache.getValue(pixivImageRedisKey);
			mode = pidAndMode.split("_")[1];
		}

		String pixivImageListRedisKey = pixivImageListKey + userId + mode;
		String pixivImageListPageNoRedisKey = pixivImageListPageNoKey + userId + mode;

		log.debug("PixivRecommendHandle get info");
		PixivRecommendIllust illust;
		while (true) {
			List<PixivRecommendIllust> illustList;
			if (redisCache.exists(pixivImageListRedisKey)) {
				illustList = (List<PixivRecommendIllust>) redisCache.getValue(pixivImageListRedisKey);
			} else {
				redisCache.delete(pixivImageListPageNoRedisKey);
				illustList = pixivManager.getRecommendImageByCookie(cookie, mode);
				redisCache.setValue(pixivImageListRedisKey, illustList);
			}
			if (CollectionUtils.isEmpty(illustList)) {
				return BotMessage.simpleTextMessage("啊嘞，怎么会找不到呢。");
			}
			int pageNo = Math.toIntExact(redisCache.increment(pixivImageListPageNoRedisKey)) - 1;
			if (pageNo >= illustList.size()) {
				redisCache.delete(pixivImageListRedisKey);
				redisCache.delete(pixivImageListPageNoRedisKey);
				continue;
			}
			illust = illustList.get(pageNo);
			boolean isSese = illust.getSl() > 5;
			// 推荐无法完全过滤色色和不色
			if ("safe".equals(mode) && isSese) {
				continue;
			} else if ("r18".equals(mode) && !isSese) {
				continue;
			}
			break;
		}

		String pid = illust.getId();
		Integer sl = illust.getSl();
		Integer pageCount = illust.getPageCount();

		log.debug("PixivRecommendHandle get make messageChainList");
		List<BotMessageChain> messageChainList = pixivService.getImageChainList(illust.getTitle(), illust.getUserName(), pid, sl, pageCount, canSS);

		log.debug("PixivRecommendHandle save result");
		redisCache.setValue(pixivImageRedisKey, pid + "_" + mode, 120);
		log.debug("PixivRecommendHandle send");
		return BotMessage.simpleListMessage(messageChainList);
	}
}
