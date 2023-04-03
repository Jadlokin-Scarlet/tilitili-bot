package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivCacheService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PidHandle extends ExceptionRespMessageHandle {
	private final PixivCacheService pixivService;
	private final BotTaskMapper botTaskMapper;
	private final BotMessageService botMessageService;
	private final PixivCacheManager pixivManager;

	@Autowired
	public PidHandle(PixivCacheService pixivService, BotTaskMapper botTaskMapper, BotMessageService botMessageService, PixivCacheManager pixivManager) {
		this.pixivService = pixivService;
		this.botTaskMapper = botTaskMapper;
		this.botMessageService = botMessageService;
		this.pixivManager = pixivManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		BotRobot bot = messageAction.getBot();
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		if (StringUtils.isBlank(pid)) {
			pid = botMessageService.getQuotePid(messageAction);
		}
		if (StringUtils.isBlank(pid)) {
			pid = pixivService.findPixivImage(botMessageService.getFirstImageListOrQuoteImage(messageAction));
		}
		Asserts.isNumber(pid, "格式错啦(pid)");

		BotSender botSender = messageAction.getBotSender();
		List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
		boolean canSS = !botTaskList.isEmpty();

		PixivInfoIllust info = pixivManager.getInfoProxy(pid);
		String title = info.getTitle();
		Integer sl = info.getSl();
		Integer pageCount = info.getPageCount();
		String userName = info.getUserName();
		List<BotMessageChain> messageChainList = pixivService.getImageChainList(bot, botSender, title, userName, pid, sl, pageCount, canSS);
		return BotMessage.simpleListMessage(messageChainList);
	}
}
