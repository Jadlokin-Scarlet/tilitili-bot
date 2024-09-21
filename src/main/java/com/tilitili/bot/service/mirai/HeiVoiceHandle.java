package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserVoiceBookmark;
import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.query.BotUserVoiceBookmarkQuery;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.hei.HeiVoice;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.manager.HeiVoiceManager;
import com.tilitili.common.mapper.mysql.BotUserVoiceBookmarkMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HeiVoiceHandle extends ExceptionRespMessageHandle {
	private static final String VOICE_NAME_MAP_KEY = "HeiVoiceHandle.voiceNameMap-";
	private final BotUserVoiceBookmarkMapper botUserVoiceBookmarkMapper;
	private final HeiVoiceManager heiVoiceManager;
	private final MusicService musicService;
	private final RedisCache redisCache;


	public HeiVoiceHandle(HeiVoiceManager heiVoiceManager, MusicService musicService, RedisCache redisCache, BotUserVoiceBookmarkMapper botUserVoiceBookmarkMapper) {
		this.botUserVoiceBookmarkMapper = botUserVoiceBookmarkMapper;
		this.heiVoiceManager = heiVoiceManager;
		this.musicService = musicService;
		this.redisCache = redisCache;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws IOException, InterruptedException {
		switch (messageAction.getKeyWithoutPrefix()) {
			case "搜索语音": return handleSearch(messageAction);
			case "收藏语音": return handleBookmark(messageAction);
			case "语音": return handleSend(messageAction);
			default: throw new AssertException();
		}
	}

	private BotMessage handleSend(BotMessageAction messageAction) {
		String search = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		BotRobot bot = messageAction.getBot();
		Long userId = botUser.getId();

		if (StringUtils.isBlank(search)) {
			List<BotUserVoiceBookmark> bookmarkList = botUserVoiceBookmarkMapper.getBotUserVoiceBookmarkByCondition(new BotUserVoiceBookmarkQuery().setUserId(userId));
			return BotMessage.simpleTextMessage("以下是已收藏语音，输入（语音 关键词）发送第一个匹配的语音\n" + bookmarkList.stream().map(BotUserVoiceBookmark::getName).collect(Collectors.joining("\n")));
		}

		try {
			String url = this.getUrlByName(search);
			PlayerMusic playerMusic = generatePlayerMusic(url, botUser);
			if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusic)) {
				return BotMessage.simpleTextMessage(String.format("已添加到播放列表，发送(收藏语音 %s)以收藏，之后就可以只输入关键词发送该语音啦！", search));
			} else {
				return BotMessage.simpleVoiceIdMessage(null, url);
			}
		} catch (AssertException e) {
			log.info(e.getMessage());
		}

		String name = botUserVoiceBookmarkMapper.searchFirstUserVoice(userId, search);
		Asserts.notNull(name, "还没有收藏相关语音，发送（收藏语音 语音名）收藏吧！收藏成功后通过关键词发送匹配的第一个语音");
		String url = this.getUrlByName(name);

		PlayerMusic playerMusic = generatePlayerMusic(url, botUser);
		if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusic)) {
			return BotMessage.simpleTextMessage("已添加到播放列表");
		} else {
			return BotMessage.simpleVoiceIdMessage(null, url);
		}
	}

	private BotMessage handleBookmark(BotMessageAction messageAction) {
		String search = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();

		Long userId = botUser.getId();
		this.getUrlByName(search);

		BotUserVoiceBookmark userBookmark = botUserVoiceBookmarkMapper.getBotUserVoiceBookmarkByUserIdAndName(userId, search);
		Asserts.checkNull(userBookmark, "已经收藏过啦");
		int cnt = botUserVoiceBookmarkMapper.addBotUserVoiceBookmarkSelective(new BotUserVoiceBookmark().setUserId(userId).setName(search));
		Asserts.checkEquals(cnt, 1);
		return BotMessage.simpleTextMessage("收藏成功，使用（语音 关键词）即可发送匹配的第一个语音");
	}

	public BotMessage handleSearch(BotMessageAction messageAction) {
		String search = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		BotRobot bot = messageAction.getBot();

		if (StringUtils.isBlank(search) || StringUtils.isNumber(search)) {
			int pageNo = StringUtils.isBlank(search)? 1: Integer.parseInt(search);
			Asserts.isRange(1, pageNo, 100, "页数不对劲");
			List<HeiVoice> voiceList = heiVoiceManager.searchHeiVoice("", pageNo);
			return BotMessage.simpleTextMessage("以下是热门语音，输入（搜索语音 页数）翻页\n" + voiceList.stream().map(HeiVoice::getName).collect(Collectors.joining("\n")));
		}

		String url = this.getUrlByName(search);

		PlayerMusic playerMusic = generatePlayerMusic(url, botUser);
		if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusic)) {
			return BotMessage.simpleTextMessage("已添加到播放列表");
        } else {
			return BotMessage.simpleVoiceIdMessage(null, url);
		}
	}

	private String getUrlByName(String search) {
		String url = redisCache.getValueString(VOICE_NAME_MAP_KEY + search);
		if (url == null) {
			List<HeiVoice> voiceList = heiVoiceManager.searchHeiVoice(search, 1);
			Asserts.notEmpty(voiceList, "没搜到");
			HeiVoice heiVoice = voiceList.stream().filter(StreamUtil.isEqual(HeiVoice::getName, search)).findFirst().orElse(voiceList.get(0));
			Asserts.checkEquals(heiVoice.getName(), search, "好像没有，以下是搜索结果：\n" + voiceList.stream().map(HeiVoice::getName).collect(Collectors.joining("\n")));
			url = String.format("https://hf.max-c.com/voice/%s.%s", heiVoice.getPath(), heiVoice.getExt());
		}
		redisCache.setValue(VOICE_NAME_MAP_KEY + search, url, TimeUnit.DAYS.toSeconds(7));
		return url;
	}

	private static @NotNull PlayerMusic generatePlayerMusic(String url, BotUserDTO botUser) {
		PlayerMusic playerMusic = new PlayerMusic();
		playerMusic.setFileUrl(url).setType(PlayerMusicDTO.TYPE_FILE).setName(String.format("%s的喊话", botUser.getName()));
		return playerMusic;
	}
}
