package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PixivLikeRecommendHandle extends ExceptionRespMessageHandle {
	private static final String pixivLikeRecommendKey = "pixivLikeRecommendKey-";
	private static final String pixivLikeRecommendIndexKey = "pixivLikeRecommendIndexKey-";
	private final PixivCacheManager pixivManager;
	private final BotTaskMapper botTaskMapper;
	private final RedisCache redisCache;
	private final BotMessageService botMessageService;
	private final PixivCacheService pixivService;

	@Autowired
	public PixivLikeRecommendHandle(PixivCacheManager pixivManager, BotTaskMapper botTaskMapper, RedisCache redisCache, BotMessageService botMessageService, PixivCacheService pixivService) {
		this.pixivManager = pixivManager;
		this.botTaskMapper = botTaskMapper;
		this.redisCache = redisCache;
		this.botMessageService = botMessageService;
		this.pixivService = pixivService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		if (StringUtils.isBlank(pid)) {
			pid = botMessageService.getQuotePid(messageAction);
		}
		if (StringUtils.isBlank(pid)) {
			pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
		}
		Asserts.isNumber(pid, "格式错啦(pid)");

		BotMessage botMessage = messageAction.getBotMessage();

		BotSender botSender = messageAction.getBotSender();
		List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
		boolean canSS = !botTaskList.isEmpty();

		log.debug("PixivRecommendHandle get info");
		PixivInfoIllust illust;
		while (true) {
			int index = Math.toIntExact(redisCache.increment(pixivLikeRecommendIndexKey + pid)) - 1;
			List<String> pidList;
			if (redisCache.exists(pixivLikeRecommendKey + pid)) {
				pidList = (List<String>) redisCache.getValue(pixivLikeRecommendKey + pid);
			} else {
				pidList = pixivManager.getLikeImageRecommend(pid);
				redisCache.setValue(pixivLikeRecommendKey + pid, pidList);
			}
			if (pidList == null || index >= pidList.size()) {
				if (index < 2) return BotMessage.simpleTextMessage("啊嘞，没找到。");
				return BotMessage.simpleTextMessage("已经看光光了。");
			}
			String recommendPid = pidList.get(index);
			try {
				illust = pixivManager.getInfoProxy(recommendPid);
			} catch (AssertException e) {
				log.error("获取相关推荐失败, pid={}, message={}", recommendPid, e.getMessage());
				continue;
			}
			if (!canSS && illust.getSl() > 3) {
				continue;
			}
			break;
		}

		String recommendPid = illust.getId();
		Integer sl = illust.getSl();
		Integer pageCount = illust.getPageCount();

		log.debug("PixivRecommendHandle get make messageChainList");
		List<BotMessageChain> messageChainList = pixivService.getImageChainList(illust.getTitle(), illust.getUserName(), pid, sl, pageCount, canSS);
		return BotMessage.simpleListMessage(messageChainList);
	}
}
