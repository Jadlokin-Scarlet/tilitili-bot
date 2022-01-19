package com.tilitili.bot.service;

import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTagTranslation;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.mapper.tilitili.PixivTagMapper;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.RedisCache;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

@Service
public class PixivService {
	public static final String source = "pixiv";
	public static final String messageIdKey = "pixiv.messageId";
	private final RedisCache redisCache;
	private final PixivImageMapper pixivImageMapper;
	private final LoliconManager loliconManager;
	private final PixivManager pixivManager;
	private final BotManager botManager;
	private final PixivTagMapper pixivTagMapper;

	@Autowired
	public PixivService(RedisCache redisCache, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager, PixivTagMapper pixivTagMapper) {
		this.redisCache = redisCache;
		this.pixivImageMapper = pixivImageMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
		this.pixivTagMapper = pixivTagMapper;
	}

	public void handlePixiv(BotMessage botMessage, String sendMessageId, String source, String searchKey, String user, String r18, String num) throws UnsupportedEncodingException, InterruptedException {
		String messageId;
		switch (source) {
			case "lolicon": messageId = sendLoliconImage(botMessage, searchKey == null? "チルノ": searchKey, source, num, r18); break;
			case "pixiv": {
				if (isNotBlank(user)) messageId = pixivManager.sendPixivUserImage(sendMessageId, user, source, r18);
				else messageId = pixivManager.sendPixivImage(sendMessageId, searchKey == null ? "チルノ 東方Project100users入り" : searchKey, source, r18);
				break;
			}
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
			String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
			if (isSese) {
				messageChainList.add(new BotMessageChain().setType("Plain").setText(ossUrl));
			} else {
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

	public void saveImageFromPixiv(String pid) throws InterruptedException {
		PixivInfoIllust info = pixivManager.getInfoProxy(pid);
		PixivImage pixivImage = new PixivImage();
		pixivImage.setPid(pid);
		pixivImage.setTitle(info.getTitle());
		pixivImage.setPageCount(info.getPageCount());
		pixivImage.setSmallUrl(info.getUrls().getOriginal());
		pixivImage.setIllustType(info.getIllustType());
		pixivImage.setUserName(info.getUserName());
		pixivImage.setUserId(info.getUserId());
		pixivImage.setSearchKey(pid);
		pixivImage.setSource(source);
		pixivImage.setSl(info.getSl());
		pixivImage.setUrlList(info.getUrls().getOriginal());
		pixivImage.setBookmark(info.getBookmarkCount());

		if (pixivImage.getPageCount() != null && pixivImage.getPageCount() > 1) {
			List<String> urlList = pixivManager.getPageListProxy(pid);
			if (urlList.size() > 6) {
				urlList = urlList.subList(0, 5);
			}
			pixivImage.setUrlList(Strings.join(urlList, ','));
		}
		pixivImageMapper.addPixivImageSelective(pixivImage);

		boolean hasTag = pixivTagMapper.countPixivTagByCondition(new PixivTagQuery().setPid(pid)) > 0;
		if (! hasTag) {
			Function<PixivInfoTag, String> getTransTag = t -> Optional.ofNullable(t.getTranslation()).map(PixivInfoTagTranslation::getEn).orElse(null);
			List<String> tagList = info.getTags().getTags().stream().flatMap(t-> Stream.of(t.getTag(), getTransTag.apply(t))).filter(Objects::nonNull).collect(Collectors.toList());
			for (String tag : tagList) {
				PixivTag addTag = new PixivTag();
				addTag.setTag(tag);
				addTag.setPid(pid);
				pixivTagMapper.addPixivTagSelective(addTag);
			}
		}
	}
}
