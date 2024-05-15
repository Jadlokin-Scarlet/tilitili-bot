package com.tilitili.bot.service.mirai;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.MusicService;
import com.tilitili.bot.service.mirai.base.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.entity.view.bot.hei.HeiVoice;
import com.tilitili.common.entity.view.bot.hei.HeiVoiceResponse;
import com.tilitili.common.entity.view.bot.hei.HeiVoiceSearchResult;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class HeiVoiceHandle extends ExceptionRespMessageHandle {
	@Value("${HeiVoiceHandle.heiCookie:}")
	private String heiCookie;

	private final MusicService musicService;

	public HeiVoiceHandle(MusicService musicService) {
		this.musicService = musicService;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) throws Exception {
		String search = messageAction.getValue();
		BotUserDTO botUser = messageAction.getBotUser();
		BotRobot bot = messageAction.getBot();
		BotSender botSender = messageAction.getBotSender();

		List<HeiVoice> voiceList = searchHeiVoice(search);
		Asserts.notEmpty(voiceList, "没搜到");
		HeiVoice heiVoice = voiceList.get(0);
		Asserts.checkEquals(heiVoice, search, "好像没有，你要找的是%s吗？", heiVoice.getName());
		String url = String.format("https://hf.max-c.com/voice/%s.mp3", heiVoice.getPath());

		PlayerMusicDTO playerMusicDTO = new PlayerMusicDTO();
		playerMusicDTO.setFileUrl(url).setType(PlayerMusicDTO.TYPE_FILE).setName(String.format("%s的喊话", botUser.getName()));
		if (musicService.pushMusicToQuote(bot, botSender, botUser, playerMusicDTO) == null) {
//            return BotMessage.simpleVoiceIdMessage(voiceId, url);
			return BotMessage.simpleTextMessage("已添加到播放列表");
        } else {
			return null;
		}
	}

	private List<HeiVoice> searchHeiVoice(String search) {
		String url = String.format("https://chat.xiaoheihe.cn/chatroom/v2/msg/voice_pack/platform/list?client_type=heybox_chat&x_client_type=web&os_type=web&x_os_type=Windows&device_info=Chrome&x_app=heybox_chat&version=999.0.3&web_version=1.0.0&chat_os_type=web&chat_version=1.21.3&offset=0&limit=1&key=%s&order=&hkey=4C93878&nonce=D51AE5C114CC7D3352E6EB01311D769F&_time=1715600258&_chat_time=1715600258005",
				URLEncoder.encode(search, StandardCharsets.UTF_8));
		String result = HttpClientUtil.httpGet(url, ImmutableMap.of("cookie", heiCookie, "User-Agent", HttpClientUtil.defaultUserAgent));
		Asserts.notBlank(result, "网络异常");
		HeiVoiceResponse<HeiVoiceSearchResult> response = Gsons.fromJson(result, new TypeToken<HeiVoiceResponse<HeiVoiceSearchResult>>(){}.getType());
		Asserts.notNull(response, "网络异常");
		Asserts.checkEquals(response.getStatus(), "ok", "请求失败(%s)", response.getMsg());
		List<HeiVoice> voiceList = response.getResult().getList();
		return voiceList;
	}
}
