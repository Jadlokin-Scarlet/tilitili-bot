package com.tilitili.bot.controller;

import com.tilitili.bot.entity.MusicSearchKeyHandleResult;
import com.tilitili.bot.entity.MusicSearchVO;
import com.tilitili.bot.entity.WebControlDataVO;
import com.tilitili.bot.entity.request.DeleteMusicRequest;
import com.tilitili.bot.entity.request.StartPlayerRequest;
import com.tilitili.bot.entity.request.SyncMusicRequest;
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
import com.tilitili.common.entity.view.bot.musiccloud.MusicCloudSong;
import com.tilitili.common.manager.*;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/api/music")
public class MusicController extends BaseController{
	private final BotSenderCacheManager botSenderCacheManager;
	private final RedisCache redisCache;
	private final MusicService musicService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final BotUserManager botUserManager;
	private final PlayerMusicMapper playerMusicMapper;
	private final MusicCloudManager musicCloudManager;
	private final BotSenderManager botSenderManager;
	private final PlayerMusicManager playerMusicManager;
	private final PlayerMusicListMapper playerMusicListMapper;

	public MusicController(PlayerMusicListMapper playerMusicListMapper, BotSenderCacheManager botSenderCacheManager, RedisCache redisCache, MusicService musicService, BotRobotCacheManager botRobotCacheManager, BotUserManager botUserManager, PlayerMusicMapper playerMusicMapper, MusicCloudManager musicCloudManager, BotSenderManager botSenderManager, PlayerMusicManager playerMusicManager) {
		this.playerMusicListMapper = playerMusicListMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.redisCache = redisCache;
		this.musicService = musicService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.botUserManager = botUserManager;
		this.playerMusicMapper = playerMusicMapper;
		this.musicCloudManager = musicCloudManager;
		this.botSenderManager = botSenderManager;
		this.playerMusicManager = playerMusicManager;
	}

	@GetMapping("/list")
	@ResponseBody
	public BaseModel<List<PlayerMusicListDTO>> listMusic(@SessionAttribute(value = "userId") Long userId) {
		List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));

		List<PlayerMusicListDTO> result = listList.stream()
				.sorted(Comparator.comparingInt(list -> PlayerMusicDTO.TYPE_FILE == list.getType() &&  "0".equals(list.getExternalId()) ? 0 : 1))
				.map(list -> {
			PlayerMusicListDTO listDTO = new PlayerMusicListDTO(list);
//			List<PlayerMusic> musicList = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setListId(listDTO.getId()));
//			listDTO.setMusicList(musicList);
			return listDTO;
		}).collect(Collectors.toList());
		return BaseModel.success(result);
	}

	@GetMapping("/last")
	@ResponseBody
	public BaseModel<PlayerMusic> getLastMusic(@SessionAttribute(value = "userId") Long userId, Long listId, Long musicId) {
		int musicCnt = playerMusicMapper.countPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId).setListId(listId).setId(musicId));
		List<PlayerMusic> lastMusic = playerMusicMapper.getPlayerMusicByCondition(new PlayerMusicQuery().setUserId(userId).setListId(listId).setId(musicId).setPageSize(1).setPageNo(ThreadLocalRandom.current().nextInt(musicCnt)+1));
		Asserts.notEmpty(lastMusic, "没有音乐了");
		return BaseModel.success(lastMusic.get(0));
	}

	@PostMapping("/list/sync")
	@ResponseBody
	public BaseModel<?> syncMusic(@SessionAttribute(value = "userId") Long userId, @RequestBody SyncMusicRequest request) {
		if (request.getListId() != null) {
			PlayerMusicList playerMusicList = playerMusicListMapper.getPlayerMusicListById(request.getListId());
			musicService.syncMusic(userId, playerMusicList);
		} else {
			musicService.syncMusic(userId);
		}
		return BaseModel.success();
	}

	@GetMapping("/search")
	@ResponseBody
	public BaseModel<MusicSearchVO> searchMusic(@SessionAttribute(value = "userId") Long userId, String searchKey) {
		MusicSearchKeyHandleResult result = musicService.handleSearchKey(searchKey, false);
		if (result.getPlayerMusicList() != null) {
			return BaseModel.success(new MusicSearchVO().setPlayerMusicList(result.getPlayerMusicList()).setPlayerMusicListDTO(result.getPlayerMusicListDTO()));
		} else if (result.getPlayerMusicListDTO() != null) {
			return BaseModel.success(new MusicSearchVO().setPlayerMusicList(result.getPlayerMusicList()).setPlayerMusicListDTO(result.getPlayerMusicListDTO()));
		} else {
			BotSender firstSender = botSenderManager.getFirstValidSender(userId);
			Asserts.notNull(firstSender);
			BotRobot bot = botRobotCacheManager.getValidBotRobotById(firstSender.getBot());
			return BaseModel.success(new MusicSearchVO().setSongList(musicCloudManager.searchMusicList(bot, searchKey)));
		}
	}

	@PostMapping("/add")
	@ResponseBody
	public BaseModel<?> addMusic(@SessionAttribute(value = "userId") Long userId, @RequestBody MusicSearchVO request) {
		if (request.getSongList() != null) {
			MusicCloudSong song = request.getSongList().get(0);
			PlayerMusic playerMusic = new PlayerMusicDTO(PlayerMusicDTO.TYPE_MUSIC_CLOUD, song);

			PlayerMusic dbMusic = playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(userId, playerMusic.getType(), playerMusic.getExternalId());
			Asserts.checkNull(dbMusic, "该歌曲已被收藏");

			PlayerMusicList list = playerMusicManager.getOrAddDefaultPlayerMusicList(userId);
			playerMusicMapper.addPlayerMusicSelective(playerMusic.setListId(list.getId()).setUserId(userId));
		} else if (request.getPlayerMusicList() != null) {
			PlayerMusic playerMusic = request.getPlayerMusicList().get(0);

			PlayerMusic dbMusic = playerMusicMapper.getPlayerMusicByUserIdAndTypeAndExternalId(userId, playerMusic.getType(), playerMusic.getExternalId());
			Asserts.checkNull(dbMusic, "该歌曲已被收藏");

			PlayerMusicList list = playerMusicManager.getOrAddDefaultPlayerMusicList(userId);
			playerMusicMapper.addPlayerMusicSelective(playerMusic.setListId(list.getId()).setUserId(userId));
		} else {
			PlayerMusicListDTO musicList = request.getPlayerMusicListDTO();

			PlayerMusicList dbList = playerMusicListMapper.getPlayerMusicListByUserIdAndTypeAndExternalId(userId, musicList.getType(), musicList.getExternalId());
			Asserts.checkNull(dbList, "该歌单已被收藏");
			playerMusicListMapper.addPlayerMusicListSelective(musicList.setUserId(userId));
			musicService.syncMusic(userId);
		}

		return BaseModel.success();
	}

	@DeleteMapping("/list")
	@ResponseBody
	public BaseModel<?> deleteMusic(@SessionAttribute(value = "userId") Long userId, @RequestBody DeleteMusicRequest request) {
		if (request.getListId() != null) {
			Long listId = request.getListId();

			PlayerMusicList musicList = playerMusicListMapper.getPlayerMusicListById(listId);
			Asserts.notNull(musicList, "参数异常");
			Asserts.checkEquals(musicList.getUserId(), userId, "参数异常");

			playerMusicListMapper.deletePlayerMusicListByPrimary(listId);
			musicService.syncMusic(userId);
		}

		return BaseModel.success();
	}

	@PostMapping("/{musicId}/volume")
	@ResponseBody
	public BaseModel<?> editMusicVolume(@SessionAttribute(value = "userId") Long userId, @PathVariable Long musicId, @RequestBody PlayerMusic request) {
		Asserts.notNull(musicId, "参数异常");
		Asserts.notNull(request, "参数异常");
		PlayerMusic playerMusic = playerMusicMapper.getPlayerMusicById(musicId);
		Asserts.notNull(playerMusic, "参数异常");
		Asserts.checkEquals(playerMusic.getUserId(), userId, "参数异常");
		int cnt = playerMusicMapper.updatePlayerMusicVolume(musicId, request.getVolume());
		Asserts.checkEquals(cnt, 1, "更新失败");
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
			if (request.getListId() == null) {
				musicService.startList(textSender, voiceSender, botUser);
			} else {
				musicService.startList(textSender, voiceSender, request.getListId());
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
