package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.FindImageResult;
import com.tilitili.common.emnus.RedisKeyEnum;
import com.tilitili.common.entity.BotPixivSendRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivImageQuery;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.entity.view.bot.mirai.MiraiUploadImageResult;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTagTranslation;
import com.tilitili.common.entity.view.bot.pixiv.PixivSearchIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.mysql.BotPixivSendRecordMapper;
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
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final BotPixivSendRecordMapper botPixivSendRecordMapper;
	private final PixivImageMapper pixivImageMapper;
	private final PixivTagMapper pixivTagMapper;
	private final LoliconManager loliconManager;
	private final PixivManager pixivManager;
	private final BotManager botManager;
	private final AtomicBoolean lockFlag = new AtomicBoolean(false);
	private final AtomicBoolean lock2Flag = new AtomicBoolean(false);

	@Autowired
	public PixivService(RedisCache redisCache, BotPixivSendRecordMapper botPixivSendRecordMapper, PixivImageMapper pixivImageMapper, LoliconManager loliconManager, PixivManager pixivManager, BotManager botManager, PixivTagMapper pixivTagMapper) {
		this.redisCache = redisCache;
		this.botPixivSendRecordMapper = botPixivSendRecordMapper;
		this.pixivImageMapper = pixivImageMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
		this.pixivTagMapper = pixivTagMapper;
	}

	public void handlePixiv(BotMessage botMessage, String source, String searchKey, String user, String r18, String num, BotSender sender) throws UnsupportedEncodingException, InterruptedException {
		String messageId;
		String searchKeyOrDefault = searchKey == null ? "チルノ" : searchKey;
		switch (source) {
			case "lolicon": messageId = sendLoliconImage(botMessage, searchKeyOrDefault, source, num, r18); break;
			case "pixiv": {
				if (isNotBlank(user)) messageId = sendPixivUserImage(botMessage, user, source, r18, sender);
				else {
					String goodSearchKey = searchKeyOrDefault.contains("users入り") ? searchKeyOrDefault : searchKeyOrDefault + " " + goodTag;
					messageId = sendPixivImage(botMessage, goodSearchKey, source, r18, sender);
					if (StringUtils.isBlank(messageId) && !searchKeyOrDefault.contains("users入り")) messageId = sendPixivImage(botMessage, searchKeyOrDefault, source, r18, sender);
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

	public String sendPixivUserImage(BotMessage quote, String userName, String source, String r18, BotSender sender) {
		// step 1 有缓存直接读缓存
		String messageId = sendCachePixivUserImage(quote, userName, source, r18, sender);
		if (messageId != null) {
			return messageId;
		}
		try {
			// step 2 爬取作者所有插画
			spiderPixivUserImage(userName);
			// step 3 从缓存中取
			messageId = sendCachePixivUserImage(quote, userName, source, r18, sender);
			return messageId;
		} finally {
			lockFlag.set(false);
		}
	}

	public String sendPixivImage(BotMessage quote, String searchKey, String source, String r18, BotSender sender) {
		// step 1 有缓存直接读缓存
		String messageId;
		try {
			log.info("searchKey={} r18={} 检查缓存", searchKey, r18);
			Asserts.isTrue(lock2Flag.compareAndSet(false, true), "猪脑过载，你先别急Σ（ﾟдﾟlll）");
			messageId = sendCachePixivImage(quote, searchKey, source, r18, sender);
			if (messageId != null) {
				return messageId;
			}
		} finally {
			lock2Flag.set(false);
		}
		try {
//			Asserts.isTrue(lockFlag.compareAndSet(false, true), "出门找图了，一会儿再来吧Σ（ﾟдﾟlll）");
			Asserts.isTrue(lockFlag.compareAndSet(false, true), "猪脑过载，你先别急Σ（ﾟдﾟlll）");
			// step 2 爬热门
			log.info("searchKey={} r18={} 检查热门第一页", searchKey, r18);
			List<PixivSearchIllust> proDataList = pixivManager.searchProProxy(searchKey, 1L, r18);
			messageId = handleSearchDataList(proDataList, quote, searchKey, source, r18, sender);
			if (messageId != null) {
				return messageId;
			}
			log.info("searchKey={} r18={} 检查热门第二页", searchKey, r18);
			proDataList = pixivManager.searchProProxy(searchKey, 2L, r18);
			messageId = handleSearchDataList(proDataList, quote, searchKey, source, r18, sender);
			if (messageId != null) {
				return messageId;
			}
			// step 3 爬第一页
			log.info("searchKey={} r18={} 检查普通第一页", searchKey, r18);
			List<PixivSearchIllust> firstDataList = pixivManager.searchProxy(searchKey, 1L);
			messageId = handleSearchDataList(firstDataList, quote, searchKey, source, r18, sender);
			if (messageId != null) {
				return messageId;
			}
			// step 4 从最新最新一页开始
			while (messageId == null) {
				Long pageNo = redisCache.increment(RedisKeyEnum.SPIDER_PIXIV_PAGENO.getKey(), searchKey);
				if (pageNo == 1L) pageNo = redisCache.increment(RedisKeyEnum.SPIDER_PIXIV_PAGENO.getKey(), searchKey);
				log.info("searchKey={} r18={} 检查普通第{}页", searchKey, r18, pageNo);
				List<PixivSearchIllust> dataList = pixivManager.searchProxy(searchKey, pageNo);
				if (dataList.isEmpty()) {
					// step 5 爬到底了，再次检查缓存
					log.info("searchKey={} r18={} 检查最终缓存", searchKey, r18);
					messageId = sendCachePixivImage(quote, searchKey, source, r18, sender);
					return messageId;
				}
				messageId = handleSearchDataList(dataList, quote, searchKey, source, r18, sender);
			}
			return messageId;
		} finally {
			lockFlag.set(false);
		}
	}

	public String sendCachePixivUserImage(BotMessage quote, String userName, String source, String r18, BotSender sender) {
		PixivImage noUsedImage = pixivImageMapper.getNoUsedUserImage(new PixivImageQuery().setUserName(userName).setSource(source).setR18(r18).setSenderId(sender.getId()));
		if (noUsedImage != null) {
			return sendPixivImage(quote, noUsedImage, sender);
		} else {
			return null;
		}
	}

	public String sendCachePixivImage(BotMessage quote, String searchKey, String source, String r18, BotSender sender) {
		List<String> searchTagList = Arrays.asList(searchKey.split(" "));
		boolean isFilterBookmark = ! searchKey.contains("goodTag");
		PixivImage noUsedImage;
		if (isFilterBookmark) {
			noUsedImage = pixivImageMapper.getNoUsedImage(new PixivImageQuery().setTagList(searchTagList).setSource(source).setR18(r18).setSenderId(sender.getId()));
		} else {
			noUsedImage = pixivImageMapper.getNoUsedImageWithoutLimit(new PixivImageQuery().setTagList(searchTagList).setSource(source).setR18(r18).setSenderId(sender.getId()));
		}
		if (noUsedImage != null) {
			return sendPixivImage(quote, noUsedImage, sender);
		} else {
			return null;
		}
	}

	public String handleSearchDataList(List<PixivSearchIllust> dataList, BotMessage quote, String searchKey, String source, String r18, BotSender sender) {
		if (CollectionUtils.isEmpty(dataList)) return null;

		List<String> searchTagList = Arrays.asList(searchKey.split(" "));
		List<String> pidList = dataList.stream().map(PixivSearchIllust::getId).collect(Collectors.toList());
		List<PixivImage> oldPixivImageList = pixivImageMapper.getPixivImageByPidList(new PixivImageQuery().setPidList(pidList).setSource(source));
		Map<String, PixivImage> oldPixivImageMap = oldPixivImageList.stream().collect(Collectors.toMap(PixivImage::getPid, Function.identity(), (a, b) -> a));
		List<PixivTag> tagList = pixivTagMapper.getPixivTagByPidList(pidList);
		Map<String, Long> tagCountMap = tagList.stream().collect(Collectors.groupingBy(PixivTag::getPid, Collectors.counting()));

		String messageId = null;
		for (PixivSearchIllust data : dataList) {
			try {
				String pid = data.getId();
				PixivImage oldPixivImage = oldPixivImageMap.get(pid);
				Long tagCount = tagCountMap.getOrDefault(pid, 0L);
				// 只补充有info, 没tag的图片
				if (oldPixivImage != null && tagCount == 0L) {
					supplePixivTag(data, searchTagList);
				}
				// 图片去重
				if (oldPixivImage != null) continue;
				saveImageFromPixiv(pid, searchKey, searchTagList);

				if (messageId == null) {
					messageId = sendCachePixivImage(quote, searchKey, source, r18, sender);
				}
			} catch (AssertException e) {
				log.error("搜索结果保存失败, pid={}, message={}", data.getId(), e.getMessage());
			}
		}
		if (messageId == null) {
			messageId = sendCachePixivImage(quote, searchKey, source, r18, sender);
		}
		return messageId;
	}

	public void spiderPixivUserImage(String userName) {
		String userId = pixivManager.getUserIdByNameProxy(userName);
		List<String> userPidList = pixivManager.getUserPidListProxy(userId);
		List<PixivImage> oldPixivImageList = pixivImageMapper.getPixivImageByPidList(new PixivImageQuery().setPidList(userPidList).setSource(source));
		Map<String, PixivImage> oldPixivImageMap = oldPixivImageList.stream().collect(Collectors.toMap(PixivImage::getPid, Function.identity()));

		for (String pid : userPidList) {
			try {
				PixivImage oldPixivImage = oldPixivImageMap.get(pid);
				if (oldPixivImage != null) continue;
				saveImageFromPixiv(pid, userName);
			} catch (AssertException e) {
				log.error("作者列表保存失败, pid={}, message={}", pid, e.getMessage());
			}
		}
	}

	public String sendPixivImage(BotMessage quote, PixivImage noUsedImage, BotSender sender) {
		String title = noUsedImage.getTitle();
		String userName = noUsedImage.getUserName();
		String pid = noUsedImage.getPid();
		Integer sl = noUsedImage.getSl();
		String[] urlList = noUsedImage.getUrlList().split(",");

		List<BotMessageChain> messageChainList = this.getImageChainList(title, userName, pid, sl, Arrays.asList(urlList), true);
//		pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setStatus(1));
		botPixivSendRecordMapper.addBotPixivSendRecordSelective(new BotPixivSendRecord().setPid(pid).setSenderId(sender.getId()));
		String messageId = botManager.sendMessage(BotMessage.simpleListMessage(messageChainList, quote).setQuote(quote.getMessageId()));
		pixivImageMapper.updatePixivImageSelective(new PixivImage().setId(noUsedImage.getId()).setMessageId(messageId));
		return messageId;
	}

	public List<BotMessageChain> getImageChainList(String title, String userName, String pid, Integer sl, List<String> urlList, Boolean canSS) {
		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("标题: "+ title));
		messageChainList.add(BotMessageChain.ofPlain("\n作者: "+ userName));
		messageChainList.add(BotMessageChain.ofPlain("\npid: "+pid));
//		messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
//		messageChainList.add(BotMessageChain.ofPlain("pid "+pid));
		if (sl == null || sl < 5) {
			for (String url : urlList) {
				MiraiUploadImageResult uploadImageResult = this.downloadPixivImageAndUploadToQQ(url);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofMiraiUploadImageResult(uploadImageResult));
			}
		} else {
			messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
			Asserts.isTrue(canSS, "不准色色");
			for (String url : urlList) {
				String ossUrl = this.downloadPixivImageAndUploadToOSS(url);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: url));
			}
		}
		return messageChainList;
	}

	private String sendLoliconImage(BotMessage reqBotMessage, String searchKey, String source, String num, String r18) throws UnsupportedEncodingException, InterruptedException {
		List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
		Asserts.notEmpty(dataList, "没库存啦！");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		if (!dataList.isEmpty()) {
			messageChainList.add(BotMessageChain.ofPlain("pid：" + dataList.get(0).getPid()));
		}
		for (int i = 0; i < dataList.size(); i++) {
			SetuData data = dataList.get(i);
			String pid = String.valueOf(data.getPid());
			String imageUrl = data.getUrls().getOriginal();
			boolean isSese = data.getTags().contains("R-18") || data.getR18();
			messageChainList.add(BotMessageChain.ofPlain("\n"));

//			String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
//			if (isSese) {
				String ossUrl = this.downloadPixivImageAndUploadToOSS(imageUrl);
				messageChainList.add(BotMessageChain.ofPlain(ossUrl != null? ossUrl: imageUrl));
//			} else {
//				messageChainList.add(BotMessageChain.ofPlain(pid + "\n"));
//				MiraiUploadImageResult miraiUploadImageResult = this.downloadPixivImageAndUploadToQQ(imageUrl);
//				messageChainList.add(BotMessageChain.ofMiraiUploadImageResult(miraiUploadImageResult));
//			}
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

	@Retryable(value= {AssertException.class},maxAttempts = 2)
	public FindImageResult findImage(String url) {
		Asserts.notBlank(url, "找不到图片");
		String html = HttpClientUtil.httpPost("https://saucenao.com/search.php?url="+url, ImmutableMap.of());
		Asserts.notBlank(html, "网络出问题惹");
		Document document = Jsoup.parse(html);
		Elements imageList = document.select(".result:not(.hidden):not(#result-hidden-notification)");
		Asserts.isFalse(imageList.isEmpty(), "没找到🤕");
		Element image = imageList.get(0);

		String rate = image.select(".resultsimilarityinfo").text();
		String imageUrl = image.select(".resulttableimage img").attr("src");
		Elements linkList = image.select(".resultcontentcolumn a");
		Asserts.notBlank(rate, "没找到😑");
		Asserts.notBlank(imageUrl, "没找到😑");
		Asserts.isFalse(linkList.isEmpty(), "没找到😑");

		String link = linkList.get(0).attr("href");
		String rateStr = rate.replace("%", "");
		if (StringUtils.isNumber(rateStr)) {
			Asserts.isTrue(Double.parseDouble(rateStr) > 40.0, "相似度过低["+rateStr+"](怪图警告)\n"+link);
			Asserts.isTrue(Double.parseDouble(rateStr) > 60.0, "相似度过低["+rateStr+"]\n"+link);
		}

		return new FindImageResult().setLink(link).setRate(rate).setImageUrl(imageUrl);
	}

	public String findPixivImage(String url) {
		FindImageResult findImageResult = this.findImage(url);
		String link = findImageResult.getLink();
		String pid = StringUtils.patten1("&illust_id=(\\d+)", link);
		Asserts.isNumber(pid, "似乎不是Pixiv图片。");
		return pid;
	}

	private MiraiUploadImageResult downloadPixivImageAndUploadToQQ(String url) {
		String urlWithoutFooter = url.split("@")[0].split("#")[0].split("\\?")[0];
		String fileName = urlWithoutFooter.substring(urlWithoutFooter.lastIndexOf("/") + 1);
		String fileType = fileName.contains(".")? fileName.substring(fileName.lastIndexOf(".")): null;
		Path path = null;
		try {
			path = Files.createTempFile("pixiv", fileType);
			log.debug("缓存文件："+path.toString());
			File file = path.toFile();
			pixivManager.downloadPixivImage(url, file);
			Asserts.isTrue(file.exists(), "啊嘞，下载失败了。");
			Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了。");
			MiraiUploadImageResult uploadImageResult = botManager.uploadImage(file);
			Asserts.notNull(uploadImageResult, "啊嘞，上传失败了。");
			Asserts.notNull(uploadImageResult.getImageId(), "啊嘞，上传失败了。");
			Asserts.notNull(uploadImageResult.getUrl(), "啊嘞，上传失败了。");
			return uploadImageResult;
		} catch (IOException e) {
			throw new AssertException("啊嘞，不对劲", e);
		} finally {
			if (path != null) {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					log.error("清理缓存失败", e);
				}
			}
		}
	}

	private String downloadPixivImageAndUploadToOSS(String url) {
		String urlWithoutFooter = url.split("@")[0].split("#")[0].split("\\?")[0];
		String fileName = urlWithoutFooter.substring(urlWithoutFooter.lastIndexOf("/") + 1);
		String fileType = fileName.contains(".")? fileName.substring(fileName.lastIndexOf(".") + 1): null;
		Path path = null;
		try {
			path = Files.createTempFile("pixiv", "." + fileType);
			File file = path.toFile();
			pixivManager.downloadPixivImage(url, file);
			Asserts.isTrue(file.exists(), "啊嘞，下载失败了。");
			Asserts.notEquals(file.length(), 0L, "啊嘞，下载失败了。");
			String ossUrl = OSSUtil.uploadOSSByImageWithType(file, fileType);
			Asserts.notNull(ossUrl, "啊嘞，上传失败了。");
			return ossUrl;
		} catch (IOException e) {
			throw new AssertException("啊嘞，不对劲", e);
		} finally {
			if (path != null) {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					log.error("清理缓存失败", e);
				}
			}
		}
	}
}
