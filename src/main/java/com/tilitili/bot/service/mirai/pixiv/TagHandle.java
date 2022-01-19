package com.tilitili.bot.service.mirai.pixiv;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.PixivImage;
import com.tilitili.common.entity.PixivTag;
import com.tilitili.common.entity.query.PixivTagQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoIllust;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTag;
import com.tilitili.common.entity.view.bot.pixiv.PixivInfoTagTranslation;
import com.tilitili.common.manager.PixivManager;
import com.tilitili.common.mapper.tilitili.PixivImageMapper;
import com.tilitili.common.mapper.tilitili.PixivTagMapper;
import com.tilitili.common.utils.Asserts;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TagHandle extends ExceptionRespMessageHandle {
	private static final String source = "pixiv";

	private final PixivManager pixivManager;
	private final PixivTagMapper pixivTagMapper;
	private final PixivImageMapper pixivImageMapper;

	@Autowired
	public TagHandle(PixivTagMapper pixivTagMapper, PixivManager pixivManager, PixivImageMapper pixivImageMapper) {
		this.pixivTagMapper = pixivTagMapper;
		this.pixivManager = pixivManager;
		this.pixivImageMapper = pixivImageMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws InterruptedException {
		String pid = messageAction.getParamOrDefault("pid", messageAction.getValue());
		Asserts.notBlank(pid, "格式错啦(pid)");
		List<PixivTag> tagList = pixivTagMapper.getPixivTagByCondition(new PixivTagQuery().setPid(pid));
		if (tagList.isEmpty()) {
			saveImageFromPixiv(pid);
			tagList = pixivTagMapper.getPixivTagByCondition(new PixivTagQuery().setPid(pid));
		}
		Asserts.notEmpty(tagList, "没找到图片");
		return BotMessage.simpleTextMessage(String.format("[%s]的tag有：%s", pid, tagList.stream().map(PixivTag::getTag).collect(Collectors.joining("    "))));
	}

	private void saveImageFromPixiv(String pid) throws InterruptedException {
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
