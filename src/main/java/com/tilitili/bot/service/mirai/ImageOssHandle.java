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
import java.util.stream.Collectors;

@Component
public class ImageOssHandle extends ExceptionRespMessageHandle {
	private final BotMessageService botMessageService;

	@Autowired
	public ImageOssHandle(BotMessageService botMessageService) {
		this.botMessageService = botMessageService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String source = messageAction.getBodyOrDefault("s", messageAction.getBodyOrDefault("source", "oss"));
		List<String> imageList = botMessageService.getImageListOrQuoteImage(messageAction);
		List<String> resultList;
		switch (source) {
			case "qq": resultList = uploadToQQ(imageList);break;
			case "oss": resultList = uploadToOSS(imageList);break;
			default: throw new AssertException("格式错啦(源)");
		}
		return BotMessage.simpleTextMessage(String.join("\n", resultList));
	}

	private List<String> uploadToOSS(List<String> imageList) {
		List<String> resultList = new ArrayList<>();
		for (String url : imageList) {
			try {
				String ossUrl = OSSUtil.uploadOSSByUrl(QQUtil.getImageUrl(url), "png", -1);
				resultList.add(ossUrl);
			} catch (AssertException e) {
				resultList.add(e.getMessage());
			} catch (Exception e) {
				resultList.add("上传失败？");
			}
		}
		return resultList;
	}

	private List<String> uploadToQQ(List<String> imageList) {
		return imageList.stream().map(QQUtil::getImageUrl).collect(Collectors.toList());
	}
}
