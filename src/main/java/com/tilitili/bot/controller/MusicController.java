package com.tilitili.bot.controller;

import com.tilitili.bot.entity.WebControlDataVO;
import com.tilitili.bot.entity.request.StartPlayerRequest;
import com.tilitili.bot.service.MusicService;
import com.tilitili.common.component.music.MusicQueueFactory;
import com.tilitili.common.component.music.MusicRedisQueue;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.PlayerMusic;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.dto.PlayerMusicListDTO;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.query.PlayerMusicQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Controller
@RequestMapping("/api/music")
public class MusicController extends BaseController{
	private final PlayerMusicListMapper playerMusicListMapper;
	private final BotSenderCacheManager botSenderCacheManager;
	private final RedisCache redisCache;
	private final MusicService musicService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final BotUserManager botUserManager;
	private final PlayerMusicMapper playerMusicMapper;

	public MusicController(PlayerMusicListMapper playerMusicListMapper, BotSenderCacheManager botSenderCacheManager, RedisCache redisCache, MusicService musicService, BotRobotCacheManager botRobotCacheManager, BotUserManager botUserManager, PlayerMusicMapper playerMusicMapper) {
		this.playerMusicListMapper = playerMusicListMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.redisCache = redisCache;
		this.musicService = musicService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.botUserManager = botUserManager;
		this.playerMusicMapper = playerMusicMapper;
	}

	@GetMapping("/list")
	@ResponseBody
	public BaseModel<List<PlayerMusicList>> listMusic(@SessionAttribute(value = "userId") Long userId) {
		List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));
		return BaseModel.success(listList);
	}

	@GetMapping("/last")
	@ResponseBody
	public BaseModel<PlayerMusic> getLastMusic(@SessionAttribute(value = "userId") Long userId) {
		int musicCnt = playerMusicMapper.countPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId));
		List<PlayerMusic> lastMusic = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId).setPageSize(1).setPageNo(ThreadLocalRandom.current().nextInt(musicCnt)));
		Asserts.notNull(lastMusic, "没有音乐了");
		return BaseModel.success(lastMusic.get(0));
	}

	@PostMapping("/sync")
	@ResponseBody
	public BaseModel<?> syncMusic(@SessionAttribute(value = "userId") Long userId) {
		musicService.syncMusic(userId);
		return BaseModel.success();
	}




	@GetMapping("/player")
	@ResponseBody
	public BaseModel<WebControlDataVO> getPlayerData(@SessionAttribute(value = "userId") Long userId) {
		BotSender botSender = botSenderCacheManager.getActiveSender(userId);
		Asserts.notNull(botSender, "你好像还没加入语音");

		WebControlDataVO response = new WebControlDataVO();

		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(botSender.getBot(), redisCache);
		PlayerMusicListDTO musicList = musicRedisQueue.getMusicList();
		if (musicList != null) {
			musicList.setMusicList(null);
		}

		response.setTheMusic(musicRedisQueue.getTheMusic());
		response.setPlayerQueue(musicRedisQueue.getPlayerQueue());
		response.setMusicList(musicList);
		// PlayerMusicDTO.STATUS_PLAY.equals(Optional.ofNullable(musicRedisQueue.getStatus()).orElse(PlayerMusicDTO.STATUS_STOP))
		response.setPlaying(PlayerMusicDTO.STATUS_PLAY.equals(musicRedisQueue.getStatus()));
		return BaseModel.success(response);
	}

	@PostMapping("/player/stop")
	@ResponseBody
	public BaseModel<?> stop(@SessionAttribute(value = "userId") Long userId) {
		BotSender voiceSender = botSenderCacheManager.getActiveSender(userId);
		Asserts.notNull(voiceSender, "你好像还没加入语音");
		BotSender textSender = musicService.getTextSenderOrNull(voiceSender);
		musicService.stopMusic(textSender, voiceSender);
		return BaseModel.success();
	}

	@PostMapping("/player/start")
	@ResponseBody
	public BaseModel<?> start(@SessionAttribute(value = "userId") Long userId, @RequestBody StartPlayerRequest request) {
		BotSender voiceSender = botSenderCacheManager.getActiveSender(userId);
		Asserts.notNull(voiceSender, "你好像还没加入语音");
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(voiceSender.getBot());
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
		BotSender textSender = musicService.getTextSenderOrNull(voiceSender);

		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(bot.getId(), redisCache);
		if (musicRedisQueue.isEmptyAll()) {
			if (request.getListId() != null) {
				musicService.startList(textSender, voiceSender, request.getListId());
			} else {
				musicService.startList(textSender, voiceSender, botUser);
			}
		}
		musicService.startMusic(textSender, voiceSender);
		return BaseModel.success();
	}

	@PostMapping("/player/last")
	@ResponseBody
	public BaseModel<?> last(@SessionAttribute(value = "userId") Long userId) {
		BotSender voiceSender = botSenderCacheManager.getActiveSender(userId);
		Asserts.notNull(voiceSender, "你好像还没加入语音");
		BotSender textSender = musicService.getTextSenderOrNull(voiceSender);
		musicService.lastMusic(textSender, voiceSender);
		return BaseModel.success();
	}
}
