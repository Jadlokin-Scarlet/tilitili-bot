package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PixivLikeRecommendHandle extends ExceptionRespMessageHandle {
	private static final String pixivLikeRecommendKey = "pixivLikeRecommendKey-";
	private static final String pixivLikeRecommendIndexKey = "pixivLikeRecommendIndexKey-";
	private final PixivManager pixivManager;
	private final BotTaskMapper botTaskMapper;
	private final RedisCache redisCache;
	private final BotMessageService botMessageService;
	private final PixivService pixivService;

	@Autowired
	public PixivLikeRecommendHandle(PixivManager pixivManager, BotTaskMapper botTaskMapper, RedisCache redisCache, BotMessageService botMessageService, PixivService pixivService) {
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
			if (!canSS && illust.getSl() > 4) {
				continue;
			}
			break;
		}

		String recommendPid = illust.getId();
		Integer sl = illust.getSl();
		log.debug("PixivRecommendHandle get url");
		List<String> urlList = pixivManager.getPageListProxy(recommendPid);

		if (urlList.size() > 10) {
			urlList = urlList.subList(0, 10);
		}

		log.debug("PixivRecommendHandle get make messageChainList");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("https://pixiv.moe/illust/"+recommendPid));
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
		return BotMessage.simpleListMessage(messageChainList, botMessage);
	}
}
