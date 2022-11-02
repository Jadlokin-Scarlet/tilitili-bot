package com.tilitili.bot.service;

import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.BotPixivSendRecordMapper;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import com.tilitili.common.mapper.mysql.PixivTagMapper;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class PixivCacheService {
	public static final String source = "pixiv";
	public static final String messageIdKey = "pixiv.messageId";
	public static final String goodTag = "users入り";
	private final RedisCache redisCache;
	private final BotPixivSendRecordMapper botPixivSendRecordMapper;
	private final PixivImageMapper pixivImageMapper;
	private final PixivTagMapper pixivTagMapper;
	private final LoliconManager loliconManager;
	private final PixivManager pixivManager;
	private final BotManager botManager;
	private final AtomicBoolean lockFlag = new AtomicBoolean(false);
	private final AtomicBoolean lock2Flag = new AtomicBoolean(false);

	@Autowired
	public PixivCacheService(RedisCache redisCache, BotPixivSendRecordMapper botPixivSendRecordMapper, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager, PixivTagMapper pixivTagMapper) {
		this.redisCache = redisCache;
		this.botPixivSendRecordMapper = botPixivSendRecordMapper;
		this.pixivImageMapper = pixivImageMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
		this.pixivTagMapper = pixivTagMapper;
	}

//	public BotMessage handlePixiv(BotMessage botMessage, String source, String searchKey, String user, String r18, String num, BotSender sender) throws UnsupportedEncodingException, InterruptedException {
//		String searchKeyOrDefault = searchKey == null ? "チルノ" : searchKey;
//		switch (source) {
//			case "lolicon": return sendLoliconImage(botMessage, searchKeyOrDefault, source, num, r18);
//			case "pixiv": {
//				if (isNotBlank(user)) return sendPixivUserImage(botMessage, user, source, r18, sender);
//				else return sendPixivImage(botMessage, searchKeyOrDefault, source, r18, sender);
//			}
//			default: throw new AssertException("没有这个平台");
//		}
//	}
//
//	private BotMessage sendPixivImage(BotMessage botMessage, String goodSearchKey, String source, String r18, BotSender botSender) {
//		return null;
//	}
//
//	public List<BotMessageChain> getImageChainList(String title, String userName, String pid, Integer sl, List<String> urlList, Boolean canSS) {
//		List<BotMessageChain> messageChainList = new ArrayList<>();
//		messageChainList.add(BotMessageChain.ofPlain("标题: "+ title));
//		messageChainList.add(BotMessageChain.ofPlain("\n作者: "+ userName));
//		messageChainList.add(BotMessageChain.ofPlain("\npid: "+pid));
////		messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
////		messageChainList.add(BotMessageChain.ofPlain("pid "+pid));
//		if (sl == null || sl < 5) {
//			for (String url : urlList) {
//				MiraiUploadImageResult uploadImageResult = this.downloadPixivImageAndUploadToQQ(url);
//				messageChainList.add(BotMessageChain.ofPlain("\n"));
//				messageChainList.add(BotMessageChain.ofMiraiUploadImageResult(uploadImageResult));
//			}
//		} else {
//			messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
//			Asserts.isTrue(canSS, "不准色色");
//			for (String url : urlList) {
//				String ossUrl = this.downloadPixivImageAndUploadToOSS(url);
//				messageChainList.add(BotMessageChain.ofPlain("\n"));
//				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: url));
//			}
//		}
//		return messageChainList;
//	}
//
//	public void saveImageFromPixiv(String pid) {
//		saveImageFromPixiv(pid, pid, Collections.emptyList());
//	}
//
//	@Retryable(value= {AssertException.class},maxAttempts = 2)
//	public FindImageResult findImage(String url) {
//		Asserts.notBlank(url, "找不到图片");
//		String html = HttpClientUtil.httpPost("https://saucenao.com/search.php?url="+url, ImmutableMap.of());
//		Asserts.notBlank(html, "网络出问题惹");
//		Document document = Jsoup.parse(html);
//		Elements imageList = document.select(".result:not(.hidden):not(#result-hidden-notification)");
//		Asserts.isFalse(imageList.isEmpty(), "没找到🤕");
//		Element image = imageList.get(0);
//
//		String rate = image.select(".resultsimilarityinfo").text();
//		String imageUrl = image.select(".resulttableimage img").attr("src");
//		Elements linkList = image.select(".resultcontentcolumn a");
//		Asserts.notBlank(rate, "没找到😑");
//		Asserts.notBlank(imageUrl, "没找到😑");
//		Asserts.isFalse(linkList.isEmpty(), "没找到😑");
//
//		String link = linkList.get(0).attr("href");
//		String rateStr = rate.replace("%", "");
//		if (StringUtils.isNumber(rateStr)) {
//			Asserts.isTrue(Double.parseDouble(rateStr) > 40.0, "相似度过低["+rateStr+"](怪图警告)\n"+link);
//			Asserts.isTrue(Double.parseDouble(rateStr) > 60.0, "相似度过低["+rateStr+"]\n"+link);
//		}
//
//		return new FindImageResult().setLink(link).setRate(rate).setImageUrl(imageUrl);
//	}
//
//	public String findPixivImage(String url) {
//		FindImageResult findImageResult = this.findImage(url);
//		String link = findImageResult.getLink();
//		String pid = StringUtils.patten1("&illust_id=(\\d+)", link);
//		Asserts.isNumber(pid, "似乎不是Pixiv图片。");
//		return pid;
//	}
}
