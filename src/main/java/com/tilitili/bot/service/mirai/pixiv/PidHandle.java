package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.PixivService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
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

	@Autowired
	public PidHandle(PixivImageMapper pixivImageMapper, PixivService pixivService) {
		this.pixivImageMapper = pixivImageMapper;
		this.pixivService = pixivService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		Asserts.notBlank(pid, "格式错啦(pid)");
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
		if (sl == null || sl < 5) {
			messageChainList.add(new BotMessageChain().setType("Plain").setText("https://pixiv.moe/illust/"+pid));
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				messageChainList.add(new BotMessageChain().setType("Plain").setText("\n"));
				messageChainList.add(new BotMessageChain().setType("Image").setUrl(ossUrl));
			}
		} else {
			messageChainList.add(new BotMessageChain().setType("Plain").setText("https://pixiv.moe/illust/"+pid));
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				messageChainList.add(new BotMessageChain().setType("Plain").setText("\n"));
				messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl));
			}
		}
		return BotMessage.simpleListMessage(messageChainList);
	}
}
