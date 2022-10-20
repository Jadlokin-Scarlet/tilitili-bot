package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
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
import java.util.Random;

@Component
public class SelfieHandle extends ExceptionRespMessageHandle {
	private final AiImageManager aiImageManager;
	private final Random random;
	private final List<String> tagList = Arrays.asList("{best quality},{{masterpiece}},{highres},original,extremely detailed 8K wallpaper,1girl,cat ears green eyes,(cirno),child,{an extremely delicate and beautiful}".split(","));
	private final List<String> badTagList = Arrays.asList("lowres,bad anatomy,bad hands,text,error,missing fngers,extra digt,fewer digits,cropped,wort quality,low quality,normal quality,jpeg artifacts,signature,watermark,username,blurry,bad feet,r18,((nswf)),((nude))".split(","));

	@Autowired
	public SelfieHandle(AiImageManager aiImageManager) {
		this.aiImageManager = aiImageManager;
		random = new Random(System.currentTimeMillis());
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		List<String> imageList = aiImageManager.getAiImageByTagList(tagList, badTagList);
		Asserts.notEmpty(imageList, "啊嘞，生成失败了");
		if (imageList.size() > 1) {
			imageList = imageList.subList(1, imageList.size());
		}
		List<BotMessageChain> respList = new ArrayList<>();
		if (random.nextInt(2) == 0) {
			respList.add(BotMessageChain.ofPlain("就，，就这一次哦。"));
		} else {
			respList.add(BotMessageChain.ofPlain("好，，好吧。"));
		}
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
