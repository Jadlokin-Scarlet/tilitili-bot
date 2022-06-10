package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.FindImageResult;
import com.tilitili.common.emnus.RedisKeyEnum;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTagTranslation;
import com.tilitili.common.entity.view.bot.pixiv.PixivSearchIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.PixivImageMapper;
import com.tilitili.common.mapper.mysql.PixivTagMapper;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tilitili.common.utils.StringUtils.isNotBlank;

@Slf4j
@Service
public class PixivService {
	public static final String source = "pixiv";
	public static final String messageIdKey = "pixiv.messageId";
	public static final String goodTag = "users入り";
	private final RedisCache redisCache;
	private final PixivImageMapper pixivImageMapper;
	private final PixivTagMapper pixivTagMapper;
	private final LoliconManager loliconManager;
	private final PixivManager pixivManager;
	private final BotManager botManager;

	@Autowired
	public PixivService(RedisCache redisCache, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager, PixivTagMapper pixivTagMapper) {
		this.redisCache = redisCache;
		this.pixivImageMapper = pixivImageMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
		this.pixivTagMapper = pixivTagMapper;
	}

	public void handlePixiv(BotMessage botMessage, String sendMessageId, String source, String searchKey, String user, String r18, String num, String pro) throws UnsupportedEncodingException, InterruptedException {
		String messageId;
		String searchKeyOrDefault = searchKey == null ? "チルノ" : searchKey;
		switch (source) {
			case "lolicon": messageId = sendLoliconImage(botMessage, searchKeyOrDefault, source, num, r18); break;
			case "pixiv": {
				if (isNotBlank(user)) messageId = sendPixivUserImage(sendMessageId, user, source, r18);
				else {
					String goodSearchKey = searchKeyOrDefault.contains("users入り") ? searchKeyOrDefault : searchKeyOrDefault + " " + goodTag;
					messageId = sendPixivImage(sendMessageId, goodSearchKey, source, r18);
					if (StringUtils.isBlank(messageId) && !searchKeyOrDefault.contains("users入り")) messageId = sendPixivImage(sendMessageId, searchKeyOrDefault, source, r18);
				}
				break;
			}
			default: throw new AssertException("没有这个平台");
		}
		if (messageId == null) {
			throw new AssertException("啊嘞，找不到了 Σ（ﾟдﾟlll）");
		}
		redisCache.setValue(messageIdKey, messageId);
	}

	public String sendPixivUserImage(String quote, String userName, String source, String r18) {
		// step 1 有缓存直接读缓存
		String messageId = sendCachePixivUserImage(quote, userName, source, r18);
		if (messageId != null) {
			return messageId;
		}
		// step 2 爬取作者所有插画
		spiderPixivUserImage(userName);
		// step 3 从缓存中取
		messageId = sendCachePixivUserImage(quote, userName, source, r18);
		return messageId;
	}

	public String sendPixivImage(String quote, String searchKey, String source, String r18) {
		// step 1 有缓存直接读缓存
		String messageId = sendCachePixivImage(quote, searchKey, source, r18);
		if (messageId != null) {
			return messageId;
		}
		// step 2 爬热门
		List<PixivSearchIllust> proDataList = pixivManager.searchProProxy(searchKey, 1L);
		messageId = handleSearchDataList(proDataList, quote, searchKey, source, r18);
		if (messageId != null) {
			return messageId;
		}
		// step 3 爬第一页
		List<PixivSearchIllust> firstDataList = pixivManager.searchProxy(searchKey, 1L);
		messageId = handleSearchDataList(firstDataList, quote, searchKey, source, r18);
		if (messageId != null) {
			return messageId;
		}
		// step 4 从最新最新一页开始
		while (messageId == null) {
			Long pageNo = redisCache.increment(RedisKeyEnum.SPIDER_PIXIV_PAGENO.getKey(), searchKey);
			if (pageNo == 1L) pageNo = redisCache.increment(RedisKeyEnum.SPIDER_PIXIV_PAGENO.getKey(), searchKey);
			List<PixivSearchIllust> dataList = pixivManager.searchProxy(searchKey, pageNo);
			if (dataList.isEmpty()) {
				// step 5 爬到底了，再次检查缓存
				messageId = sendCachePixivImage(quote, searchKey, source, r18);
				return messageId;
			}
			messageId = handleSearchDataList(dataList, quote, searchKey, source, r18);
		}
		return messageId;
	}

	public String sendCachePixivUserImage(String quote, String userName, String source, String r18) {
		PixivImage noUsedImage = pixivImageMapper.getNoUsedUserImage(new PixivImageQuery().setUserName(userName).setSource(source).setR18(r18));
		if (noUsedImage != null) {
			return sendPixivImage(quote, noUsedImage);
		} else {
			return null;
		}
	}

	public String sendCachePixivImage(String quote, String searchKey, String source, String r18) {
		List<String> searchTagList = Arrays.asList(searchKey.split(" "));
		boolean isFilterBookmark = ! searchKey.contains("goodTag");
		PixivImage noUsedImage;
		if (isFilterBookmark) {
			noUsedImage = pixivImageMapper.getNoUsedImage(new PixivImageQuery().setTagList(searchTagList).setSource(source).setR18(r18));
		} else {
			noUsedImage = pixivImageMapper.getNoUsedImageWithoutLimit(new PixivImageQuery().setTagList(searchTagList).setSource(source).setR18(r18));
		}
		if (noUsedImage != null) {
			return sendPixivImage(quote, noUsedImage);
		} else {
			return null;
		}
	}

	public String handleSearchDataList(List<PixivSearchIllust> dataList, String quote, String searchKey, String source, String r18) {
		List<String> searchTagList = Arrays.asList(searchKey.split(" "));
		String messageId = null;
		for (PixivSearchIllust data : dataList) {
			try {
				String pid = data.getId();
				supplePixivTag(data, searchTagList);
				saveImageFromPixiv(pid, searchKey, searchTagList);

				if (messageId == null) {
					messageId = sendCachePixivImage(quote, searchKey, source, r18);
				}
			} catch (AssertException e) {
				log.error("搜索结果保存失败, pid={}, message={}", data.getId(), e.getMessage());
			}
		}
		if (messageId == null) {
			messageId = sendCachePixivImage(quote, searchKey, source, r18);
		}
		return messageId;
	}

	public void spiderPixivUserImage(String userName) {
		String userId = pixivManager.getUserIdByNameProxy(userName);
		List<String> userPidList = pixivManager.getUserPidListProxy(userId);
		for (String pid : userPidList) {
			try {
				saveImageFromPixiv(pid, userName);
			} catch (AssertException e) {
				log.error("作者列表保存失败, pid={}, message={}", pid, e.getMessage());
			}
		}
	}

	public String sendPixivImage(String quote, PixivImage noUsedImage) {
		String pid = noUsedImage.getPid();
		Integer sl = noUsedImage.getSl();
		String[] urlList = noUsedImage.getUrlList().split(",");

		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("https://pixiv.moe/illust/"+pid));
//		messageChainList.add(BotMessageChain.ofPlain("pid "+pid));
		if (sl == null || sl < 5) {
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				Asserts.notNull(ossUrl, "上传OSS失败");
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofImage(ossUrl));
			}
		} else {
			for (String url : urlList) {
				String ossUrl = OSSUtil.uploadSOSSByUrl(url);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: url));
			}
		}
		pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setStatus(1));
		String messageId = botManager.sendMessage(BotMessage.simpleListMessage(messageChainList).setQuote(quote));
		pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setMessageId(messageId));
		return messageId;
	}

	private String sendLoliconImage(BotMessage reqBotMessage, String searchKey, String source, String num, String r18) throws UnsupportedEncodingException, InterruptedException {
		List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
		Asserts.notEmpty(dataList, "没库存啦！");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		for (int i = 0; i < dataList.size(); i++) {
			SetuData data = dataList.get(i);
			String pid = String.valueOf(data.getPid());
			String imageUrl = data.getUrls().getOriginal();
			boolean isSese = data.getTags().contains("R-18") || data.getR18();
			if (i != 0) {
				messageChainList.add(BotMessageChain.ofPlain("\n"));
			}
			String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
			if (isSese) {
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: imageUrl));
			} else {
				messageChainList.add(BotMessageChain.ofPlain(pid + "\n"));
				Asserts.notNull(ossUrl, "上传OSS失败");
				messageChainList.add(BotMessageChain.ofImage(ossUrl));
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

	public void saveImageFromPixiv(String pid) {
		saveImageFromPixiv(pid, pid, Collections.emptyList());
	}

	public void saveImageFromPixiv(String pid, String searchKey) {
		saveImageFromPixiv(pid, searchKey, Collections.emptyList());
	}

	public void saveImageFromPixiv(String pid, String searchKey, List<String> externalTagList) throws AssertException {
		List<PixivImage> oldDataList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setPid(pid).setSource(source));
		if (! oldDataList.isEmpty()) return;

		PixivInfoIllust info = pixivManager.getInfoProxy(pid);
		PixivImage pixivImage = new PixivImage();
		pixivImage.setPid(pid);
		pixivImage.setTitle(info.getTitle());
		pixivImage.setPageCount(info.getPageCount());
		pixivImage.setSmallUrl(info.getUrls().getOriginal());
		pixivImage.setIllustType(info.getIllustType());
		pixivImage.setUserName(info.getUserName());
		pixivImage.setUserId(info.getUserId());
		pixivImage.setSearchKey(searchKey);
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
			// p站tag，p站翻译tag(不包含翻译中带空格的) 搜索词tag列表 去重作为最终tag列表
			Function<PixivInfoTag, String> getTransTag = t -> Optional.ofNullable(t.getTranslation()).map(PixivInfoTagTranslation::getEn).orElse(null);
			List<String> tagList = info.getTags().getTags().stream().flatMap(t-> Stream.of(t.getTag(), getTransTag.apply(t))).filter(Objects::nonNull).filter(s->!s.contains(" ")).distinct().collect(Collectors.toList());
			for (String searchTag : externalTagList) {
				if (! tagList.contains(searchTag)) tagList.add(searchTag);
			}
			for (String tag : tagList) {
				PixivTag addTag = new PixivTag();
				addTag.setTag(tag);
				addTag.setPid(pid);
				pixivTagMapper.addPixivTagSelective(addTag);
			}
		}
	}

	public void supplePixivTag(PixivSearchIllust data, List<String> externalTagList) {
		String pid = data.getId();
		// 只补充有info的图片
		List<PixivImage> oldDataList = pixivImageMapper.getPixivImageByCondition(new PixivImageQuery().setPid(pid).setSource(source));
		if (oldDataList.isEmpty()) return;
		// 只补充没tag的图片
		boolean hasTag = pixivTagMapper.countPixivTagByCondition(new PixivTagQuery().setPid(pid)) > 0;
		if (hasTag) return;

		// p站tag 搜索词tag列表 去重作为最终tag列表
		List<String> tagList = new ArrayList<>(data.getTags());
		for (String searchTag : externalTagList) {
			if (! tagList.contains(searchTag)) tagList.add(searchTag);
		}

		for (String tag : tagList) {
			PixivTag addTag = new PixivTag();
			addTag.setTag(tag);
			addTag.setPid(pid);
			pixivTagMapper.addPixivTagSelective(addTag);
		}
	}

	public FindImageResult findImage(String url) {
		Asserts.notBlank(url, "找不到图片");
		String html = HttpClientUtil.httpPost("https://saucenao.com/search.php?url="+url, ImmutableMap.of());
		Asserts.notBlank(html, "没要到图😇\n"+url);
		Document document = Jsoup.parse(html);
		Elements imageList = document.select(".result:not(.hidden):not(#result-hidden-notification)");
		Asserts.isFalse(imageList.isEmpty(), "没找到🤕\n"+url);
		Element image = imageList.get(0);

		String rate = image.select(".resultsimilarityinfo").text();
		String imageUrl = image.select(".resulttableimage img").attr("src");
		Elements linkList = image.select(".resultcontentcolumn a.linkify");
		Asserts.notBlank(rate, "没找到😑\n"+url);
		Asserts.notBlank(imageUrl, "没找到😑\n"+url);
		Asserts.isFalse(linkList.isEmpty(), "没找到😑\n"+url);

		String link = linkList.get(0).attr("href");
		String rateStr = rate.replace("%", "");
		if (StringUtils.isNumber(rateStr)) {
			Asserts.isTrue(Double.parseDouble(rateStr) > 40.0, "相似度过低(怪图警告)\n"+link);
			Asserts.isTrue(Double.parseDouble(rateStr) > 60.0, "相似度过低\n"+link);
		}

		return new FindImageResult().setLink(link).setRate(rate).setImageUrl(imageUrl);
	}

	public String findPixivImage(String url) {
		FindImageResult findImageResult = this.findImage(url);
		String link = findImageResult.getLink();
		return StringUtils.patten1("&illust_id=(\\d+)", link);
	}
}
