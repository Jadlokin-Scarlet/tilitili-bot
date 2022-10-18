package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageToSenderHandle;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.manager.AiImageManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiImageHandle extends ExceptionRespMessageToSenderHandle {
	private final AiImageManager aiImageManager;

	@Autowired
	public AiImageHandle(AiImageManager aiImageManager) {
		this.aiImageManager = aiImageManager;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String tagListStr = messageAction.getValue();
		Asserts.notBlank(tagListStr, "格式错啦(tag)");
		String[] tagList = tagListStr.split("[,，]");
		List<String> enTagList = Arrays.stream(tagList).map(aiImageManager::translateUserWord).collect(Collectors.toList());
		enTagList.addAll(AiImageManager.goodTagList);
		List<String> imageList = aiImageManager.getAiImageByTagList(enTagList, AiImageManager.bedTagList);
		Asserts.notEmpty(imageList, "啊嘞，生成失败了");
		if (imageList.size() > 1) {
			imageList = imageList.subList(1, imageList.size());
		}
		List<BotMessageChain> respList = new ArrayList<>();
		for (String imageData : imageList) {
			String base64Image = imageData.split(",")[1];
			byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64Image);
			BufferedImage buffImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				ImageIO.write(buffImage, "png", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
				try (InputStream stream = new ByteArrayInputStream(os.toByteArray())) {
					String ossUrl = OSSUtil.uploadOSSBySteam(stream);
					respList.add(BotMessageChain.ofImage(ossUrl));
				}
			}
		}
		return BotMessage.simpleListMessage(respList).setQuote(messageAction.getMessageId());
	}
}
