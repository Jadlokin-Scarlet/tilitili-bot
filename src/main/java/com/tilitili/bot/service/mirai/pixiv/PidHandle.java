package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotTask;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.mysql.BotTaskMapper;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PidHandle extends ExceptionRespMessageHandle {
	private final PixivImageMapper pixivImageMapper;
	private final PixivService pixivService;
	private final BotTaskMapper botTaskMapper;
	private final BotMessageService botMessageService;

	@Autowired
	public PidHandle(PixivImageMapper pixivImageMapper, PixivService pixivService, BotTaskMapper botTaskMapper, BotMessageService botMessageService) {
		this.pixivImageMapper = pixivImageMapper;
		this.pixivService = pixivService;
		this.botTaskMapper = botTaskMapper;
		this.botMessageService = botMessageService;
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

		BotSender botSender = messageAction.getBotSender();
		List<BotTask> botTaskList = botTaskMapper.getBotTaskListBySenderIdAndKey(botSender.getId(), "ss", "");
		boolean canSS = !botTaskList.isEmpty();

		List<PixivImage> imageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setSource("pixiv").setPid(pid));
		if (imageList.isEmpty()) {
			pixivService.saveImageFromPixiv(pid);
			imageList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setSource("pixiv").setPid(pid));
		}
		Asserts.checkEquals(imageList.size(), 1, "没找到。");
		PixivImage pixivImage = imageList.get(0);

		Integer sl = pixivImage.getSl();
		String[] urlList = pixivImage.getUrlList().split(",");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("标题: "+pixivImage.getTitle()));
		messageChainList.add(BotMessageChain.ofPlain("\n作者: "+pixivImage.getUserName()));
		messageChainList.add(BotMessageChain.ofPlain("\n镜像站: https://pixiv.moe/illust/"+pid));
		if (sl == null || sl < 5) {
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				Asserts.notNull(ossUrl, "上传OSS失败");
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofImage(ossUrl));
			}
		} else {
			messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
			Asserts.isTrue(canSS, "不准色色");
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: url));
			}
		}
		return BotMessage.simpleListMessage(messageChainList);
	}
}
