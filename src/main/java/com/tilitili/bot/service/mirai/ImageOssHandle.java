package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.BotMessageService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.QQUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageOssHandle extends ExceptionRespMessageHandle {
	private final BotMessageService botMessageService;

	@Autowired
	public ImageOssHandle(BotMessageService botMessageService) {
		this.botMessageService = botMessageService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		List<String> imageList = botMessageService.getImageListOrQuoteImage(messageAction);
		List<String> resultList = new ArrayList<>();
		for (String url : imageList) {
			try {
				String ossUrl = OSSUtil.uploadOSSByUrlAndType(QQUtil.getImageUrl(url), "png");
				resultList.add(ossUrl);
			} catch (AssertException e) {
				resultList.add(e.getMessage());
			} catch (Exception e) {
				resultList.add("上传失败？");
			}
		}
		return BotMessage.simpleTextMessage(String.join("\n", resultList));
	}
}
