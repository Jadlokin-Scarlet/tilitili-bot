package com.tilitili.bot.service;

import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

@Service
public class PixivService {
	public static final String messageIdKey = "pixiv.messageId";
	private final RedisCache redisCache;
	private final PixivImageMapper pixivImageMapper;
	private final LoliconManager loliconManager;
	private final PixivManager pixivManager;
	private final BotManager botManager;

	@Autowired
	public PixivService(RedisCache redisCache, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager) {
		this.redisCache = redisCache;
		this.pixivImageMapper = pixivImageMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
	}

	public void handlePixiv(BotMessage botMessage, String sendMessageId, String source, String searchKey, String user, String r18, String num) throws UnsupportedEncodingException, InterruptedException {
		if (Objects.equals(source, "pixiv") && isNotBlank(user)) {
			source = "powner";
		}

		String messageId;
		switch (source) {
			case "lolicon": messageId = sendLoliconImage(botMessage, searchKey == null? "チルノ": searchKey, source, num, r18); break;
			case "pixiv": messageId = pixivManager.sendPixivImage(sendMessageId, searchKey == null? "チルノ 東方Project100users入り": searchKey, source, r18); break;
			case "powner": messageId = pixivManager.sendPixivUserImage(sendMessageId, searchKey == null? "ke-ta": searchKey, source, r18); break;
			default: throw new AssertException("没有这个平台");
		}
		if (messageId != null) {
			redisCache.setValue(messageIdKey, messageId);
		}
	}

	private String sendLoliconImage(BotMessage reqBotMessage, String searchKey, String source, String num, String r18) throws InterruptedException, UnsupportedEncodingException {
		List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
		if (dataList.isEmpty()) {
			botManager.sendMessage(BotMessage.simpleTextMessage("没库存啦！", reqBotMessage));
			return null;
		}
		List<BotMessageChain> messageChainList = new ArrayList<>();
		for (int i = 0; i < dataList.size(); i++) {
			SetuData data = dataList.get(i);
			String pid = String.valueOf(data.getPid());
			String imageUrl = data.getUrls().getOriginal();
			boolean isSese = data.getTags().contains("R-18") || data.getR18();
			if (i != 0) {
				messageChainList.add(new BotMessageChain().setType("Plain").setText("\n"));
			}
			if (isSese) {
				String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
				messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl));
			} else {
				String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
				messageChainList.add(new BotMessageChain().setType("Plain").setText(pid + "\n"));
				messageChainList.add(new BotMessageChain().setType("Image").setUrl(ossUrl));
			}
		}

		String messageId = botManager.sendMessage(BotMessage.simpleListMessage(messageChainList, reqBotMessage).setQuote(reqBotMessage.getQuote()));

		for (SetuData data : dataList) {
			String pid = String.valueOf(data.getPid());
			String imageUrl = data.getUrls().getOriginal();

			PixivImage pixivImage = new PixivImage();
			pixivImage.setPid(pid);
			pixivImage.setTitle(data.getTitle());
			pixivImage.setPageCount(1);
			pixivImage.setSmallUrl(imageUrl);
			pixivImage.setUserId(String.valueOf(data.getUid()));
			pixivImage.setUrlList(imageUrl);
			pixivImage.setSearchKey(searchKey);
			pixivImage.setSource(source);
			pixivImage.setMessageId(messageId);
			pixivImage.setStatus(1);
			pixivImageMapper.addPixivImageSelective(pixivImage);
		}
		return messageId;
	}

}
