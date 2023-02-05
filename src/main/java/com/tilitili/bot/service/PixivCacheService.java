package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.FindImageResult;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.common.entity.BotPixivSendRecord;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.BotMessageChain;
import com.tilitili.common.entity.view.bot.lolicon.SetuData;
import com.tilitili.common.entity.view.bot.mirai.MiraiUploadImageResult;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivSearchIllust;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.exception.AssertSeseException;
import com.tilitili.common.manager.BotManager;
import com.tilitili.common.manager.LoliconManager;
import com.tilitili.common.manager.PixivCacheManager;
import com.tilitili.common.mapper.mysql.BotPixivSendRecordMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.OSSUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PixivCacheService {
	public static final String goodTag = "users入り";
	private final BotPixivSendRecordMapper botPixivSendRecordMapper;
	private final LoliconManager loliconManager;
	private final PixivCacheManager pixivManager;
	private final BotManager botManager;
	private final AtomicBoolean lockFlag = new AtomicBoolean(false);
	private final AtomicBoolean lock2Flag = new AtomicBoolean(false);

	@Autowired
	public PixivCacheService(BotPixivSendRecordMapper botPixivSendRecordMapper, LoliconManager loliconManager, PixivCacheManager pixivManager, BotManager botManager) {
		this.botPixivSendRecordMapper = botPixivSendRecordMapper;
		this.loliconManager = loliconManager;
		this.pixivManager = pixivManager;
		this.botManager = botManager;
	}

	public BotMessage handlePixiv(BotMessageAction messageAction, String source, String searchKey, String user, String r18, String num) throws UnsupportedEncodingException {
		try {
			Asserts.isTrue(lockFlag.compareAndSet(false, true), "猪脑过载，你先别急Σ（ﾟдﾟlll）");
			String searchKeyOrDefault = searchKey == null ? "チルノ" : searchKey;
			switch (source) {
				case "lolicon":
					return sendLoliconImage(messageAction, searchKeyOrDefault, num, r18);
				case "pixiv": {
					if (StringUtils.isNotBlank(user)) return sendPixivUserImage(messageAction, user, r18);
					else return sendPixivImage(messageAction, searchKeyOrDefault, r18);
				}
				default:
					throw new AssertException("没有这个平台");
			}
		} finally {
			lockFlag.set(false);
		}
	}

	private BotMessage sendPixivImage(BotMessageAction messageAction, String searchKey, String r18) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		String messageId = messageAction.getMessageId();
		Long senderId = botSender.getId();
		Long userId = botUser.getId();
		boolean canSS = !"safe".equals(r18);

		for (long pageNo = 1L; pageNo < 10; pageNo++) {
			List<PixivSearchIllust> dataList = pixivManager.searchProProxy(searchKey, pageNo, r18);
			if (CollectionUtils.isEmpty(dataList)) break;
			List<String> pidList = dataList.stream().map(PixivSearchIllust::getId).collect(Collectors.toList());
			List<BotPixivSendRecord> recordList = botPixivSendRecordMapper.getPixivSendRecordByPidList(pidList, senderId, userId);
			List<String> recordPidList = recordList.stream().map(BotPixivSendRecord::getPid).collect(Collectors.toList());

			for (PixivSearchIllust data : dataList) {
				String pid = data.getId();
				String title = data.getTitle();
				String userName = data.getUserName();
				Integer sl = data.getSl();
				Integer pageCount = data.getPageCount();
				// 看过了
				if (recordPidList.contains(pid)) {
					continue;
				}

				List<String> urlList = pixivManager.getPageListProxy(pid);
//				if (urlList.size() > 1) {
//					urlList = urlList.subList(0, 1);
//				}

				List<BotMessageChain> messageChainList;
				try {
					messageChainList = this.getImageChainList(title, userName, pid, sl, urlList, pageCount, canSS);
				} catch (AssertSeseException e) {
					log.warn(e.getMessage(), e);
					continue;
				}

				botPixivSendRecordMapper.addBotPixivSendRecordSelective(new BotPixivSendRecord().setPid(pid).setSenderId(senderId).setUserId(userId));
				return BotMessage.simpleListMessage(messageChainList);//.setQuote(messageId);
			}
		}
		return BotMessage.simpleTextMessage("啊嘞，似乎没有了？");
	}


	private BotMessage sendLoliconImage(BotMessageAction messageAction, String searchKey, String num, String r18) throws UnsupportedEncodingException {
		List<SetuData> dataList = loliconManager.getAImage(searchKey, num, r18);
		Asserts.notEmpty(dataList, "没库存啦！");
		List<BotMessageChain> messageChainList = new ArrayList<>();
		if (!dataList.isEmpty()) {
			messageChainList.add(BotMessageChain.ofPlain("pid：" + dataList.get(0).getPid()));
		}
		for (SetuData data : dataList) {
			String imageUrl = data.getUrls().getOriginal();
//			boolean isSese = data.getTags().contains("R-18") || data.getR18();
			messageChainList.add(BotMessageChain.ofPlain("\n"));

//			String ossUrl = OSSUtil.getCacheSOSSOrUploadByUrl(imageUrl);
//			if (isSese) {
			String ossUrl = this.downloadPixivImageAndUploadToOSS(imageUrl, 1);
			messageChainList.add(BotMessageChain.ofLink(ossUrl != null ? ossUrl : imageUrl));
//			} else {
//				messageChainList.add(BotMessageChain.ofPlain(pid + "\n"));
//				MiraiUploadImageResult miraiUploadImageResult = this.downloadPixivImageAndUploadToQQ(imageUrl);
//				messageChainList.add(BotMessageChain.ofMiraiUploadImageResult(miraiUploadImageResult));
//			}
		}

		return BotMessage.simpleListMessage(messageChainList);//.setQuote(messageAction.getMessageId());
	}

	private BotMessage sendPixivUserImage(BotMessageAction messageAction, String userName, String r18) {
		BotSender botSender = messageAction.getBotSender();
		BotUserDTO botUser = messageAction.getBotUser();
		Long senderId = botSender.getId();
		Long botUserId = botUser.getId();
		boolean canSS = !"safe".equals(r18);

		String userId = pixivManager.getUserIdByNameProxy(userName);
		List<String> userPidList = pixivManager.getUserPidListProxy(userId);
		List<BotPixivSendRecord> recordList = botPixivSendRecordMapper.getPixivSendRecordByPidList(userPidList, senderId, botUserId);
		List<String> recordPidList = recordList.stream().map(BotPixivSendRecord::getPid).collect(Collectors.toList());
		userPidList.removeAll(recordPidList);

		for (String pid : userPidList) {
			PixivInfoIllust info = pixivManager.getInfoProxy(pid);
			String title = info.getTitle();
			Integer sl = info.getSl();
			Integer pageCount = info.getPageCount();
			List<String> urlList = Collections.singletonList(info.getUrls().getOriginal());

//			if (pageCount != null && pageCount > 1) {
//				urlList = pixivManager.getPageListProxy(pid);
//				if (urlList.size() > 1) {
//					urlList = urlList.subList(0, 1);
//				}
//			}
			List<BotMessageChain> messageChainList;
			try {
				messageChainList = this.getImageChainList(title, userName, pid, sl, urlList, pageCount, canSS);
			} catch (AssertSeseException e) {
				log.warn(e.getMessage(), e);
				continue;
			}

			botPixivSendRecordMapper.addBotPixivSendRecordSelective(new BotPixivSendRecord().setPid(pid).setSenderId(senderId).setUserId(botUserId));
			return BotMessage.simpleListMessage(messageChainList);//.setQuote(messageAction.getMessageId());
		}
		return BotMessage.simpleTextMessage("啊嘞，似乎没有了？");
	}

	public List<BotMessageChain> getImageChainList(String title, String userName, String pid, Integer sl, List<String> urlList, Integer pageCount, Boolean canSS) {
		urlList = urlList.subList(0, Math.min(5, urlList.size()));

		List<BotMessageChain> messageChainList = new ArrayList<>();
		messageChainList.add(BotMessageChain.ofPlain("标题: "+ title));
		messageChainList.add(BotMessageChain.ofPlain("\n作者: "+ userName));
		messageChainList.add(BotMessageChain.ofPlain("\n页数: "+pageCount));
		messageChainList.add(BotMessageChain.ofPlain("\npid: "+pid));
		if (sl == null || sl < 3) {
			for (String url : urlList) {
				MiraiUploadImageResult uploadImageResult = this.downloadPixivImageAndUploadToQQ(url, pageCount);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofMiraiUploadImageResult(uploadImageResult));
			}
		} else {
//			messageChainList.add(BotMessageChain.ofPlain("\n原图: "));
			if (sl > 3 && !canSS) {
				throw new AssertSeseException();
//				Asserts.isTrue(canSS, "不准色色");
			}
			for (String url : urlList) {
				String ossUrl = this.downloadPixivImageAndUploadToOSS(url, pageCount);
				messageChainList.add(BotMessageChain.ofPlain("\n"));
				messageChainList.add(BotMessageChain.ofLink(ossUrl != null? ossUrl: url));
			}
		}
		return messageChainList;
	}

//	public void saveImageFromPixiv(String pid) {
//		saveImageFromPixiv(pid, pid, Collections.emptyList());
//	}

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


	private MiraiUploadImageResult downloadPixivImageAndUploadToQQ(String url, Integer pageCount) {
		log.info("downloadPixivImageAndUploadToQQ pageCount={} url={}", pageCount, url);
//		List<String> list = StringUtils.extractList("/(\\d+)_(p|ugoira)(\\d+)\\.(\\w+)", url);
//		if (pageCount > 1) {
//			int page = Integer.parseInt(list.get(2)) + 1;
//			return new MiraiUploadImageResult().setUrl(String.format("https://pixiv.nl/%s-%s.%s", list.get(0), page, list.get(3)));
//		} else {
//			return new MiraiUploadImageResult().setUrl(String.format("https://pixiv.nl/%s.%s", list.get(0), list.get(3)));
//		}
//
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

	private String downloadPixivImageAndUploadToOSS(String url, Integer pageCount) {
		log.info("downloadPixivImageAndUploadToOSS pageCount={} url={}", pageCount, url);
//		List<String> list = StringUtils.extractList("/(\\d+)_(p|ugoira)(\\d+)\\.(\\w+)", url);
//		if (pageCount > 1) {
//			int page = Integer.parseInt(list.get(2)) + 1;
//			return String.format("https://pixiv.nl/%s-%s.%s", list.get(0), page, list.get(3));
//		} else {
//			return String.format("https://pixiv.nl/%s.%s", list.get(0), list.get(3));
//		}

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
