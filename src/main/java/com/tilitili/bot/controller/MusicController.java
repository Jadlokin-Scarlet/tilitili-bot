package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.entity.response.ListPlayerMusicResponse;
import com.tilitili.bot.service.MusicService;
import com.tilitili.common.component.music.MusicQueueFactory;
import com.tilitili.common.component.music.MusicRedisQueue;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api/music")
public class MusicController extends BaseController{
	private final MusicService musicService;
	private final PlayerMusicMapper playerMusicMapper;
	private final PlayerMusicListMapper playerMusicListMapper;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderCacheManager botSenderCacheManager;
	private final BotRobotCacheManager botRobotCacheManager;
	private final RedisCache redisCache;

	public MusicController(MusicService musicService, PlayerMusicMapper playerMusicMapper, PlayerMusicListMapper playerMusicListMapper, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderCacheManager botSenderCacheManager, BotRobotCacheManager botRobotCacheManager, RedisCache redisCache) {
		this.musicService = musicService;
		this.playerMusicMapper = playerMusicMapper;
		this.playerMusicListMapper = playerMusicListMapper;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.botRobotCacheManager = botRobotCacheManager;
		this.redisCache = redisCache;
	}

	@GetMapping("/list")
	@ResponseBody
	public BaseModel<List<PlayerMusicList>> listMusic(@SessionAttribute(value = "botUser") BotUserVO botUser) {
		List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(botUser.getId()));
		return BaseModel.success(listList);
	}

	@GetMapping("/player")
	@ResponseBody
	public BaseModel<ListPlayerMusicResponse> getPlayerData(@SessionAttribute(value = "botUser") BotUserVO botUser) {
		List<BotUserSenderMapping> mappingList = botUserSenderMappingMapper.listOnlineMappingBySendTypeAndUserId(SendTypeEnum.KOOK_MESSAGE_STR, botUser.getId());
		BotUserSenderMapping mapping = mappingList.stream().findFirst().orElse(null);
		Asserts.notNull(mapping, "你好像还没加入语音");
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(mapping.getSenderId());
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(botSender.getBot());
		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(bot, redisCache);
		PlayerMusicDTO theMusic = musicRedisQueue.getTheMusic();
		ListPlayerMusicResponse response = new ListPlayerMusicResponse();
		response.setTheMusic(theMusic);
		return BaseModel.success(response);
	}
}
