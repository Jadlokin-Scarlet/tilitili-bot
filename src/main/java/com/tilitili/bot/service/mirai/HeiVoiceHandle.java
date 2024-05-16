package com.tilitili.bot.service.mirai;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.hei.HeiVoice;
import com.tilitili.common.manager.HeiVoiceManager;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HeiVoiceHandle extends ExceptionRespMessageHandle {
	private final HeiVoiceManager heiVoiceManager;
	private final MusicService musicService;

	public HeiVoiceHandle(HeiVoiceManager heiVoiceManager, MusicService musicService) {
		this.heiVoiceManager = heiVoiceManager;
		this.musicService = musicService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String search = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		BotSender botSender = messageAction.getBotSender();
		BotRobot bot = messageAction.getBot();

		if (StringUtils.isBlank(search) || StringUtils.isNumber(search)) {
			int pageNo = StringUtils.isBlank(search)? 1: Integer.parseInt(search);
			Asserts.isRange(1, pageNo, 100, "页数不对劲");
			List<HeiVoice> voiceList = heiVoiceManager.searchHeiVoice("", pageNo);
			return BotMessage.simpleTextMessage("以下是热门语音，输入（语音 页数）翻页\n" + voiceList.stream().map(HeiVoice::getName).collect(Collectors.joining("\n")));
		}

		List<HeiVoice> voiceList = heiVoiceManager.searchHeiVoice(search, 1);
		Asserts.notEmpty(voiceList, "没搜到");
		HeiVoice heiVoice = voiceList.stream().filter(StreamUtil.isEqual(HeiVoice::getName, search)).findFirst().orElse(voiceList.get(0));
		Asserts.checkEquals(heiVoice.getName(), search, "好像没有，以下是搜索结果：\n" + voiceList.stream().map(HeiVoice::getName).collect(Collectors.joining("\n")));
		String url = String.format("https://hf.max-c.com/voice/%s.%s", heiVoice.getPath(), heiVoice.getExt());

		PlayerMusicDTO playerMusicDTO = new PlayerMusicDTO();
		playerMusicDTO.setFileUrl(url).setType(PlayerMusicDTO.TYPE_FILE).setName(String.format("%s的喊话", botUser.getName()));
		if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusicDTO) == null) {
//            return BotMessage.simpleVoiceIdMessage(voiceId, url);
			return BotMessage.simpleTextMessage("已添加到播放列表");
        } else {
			return null;
		}
	}
}
