package com.tilitili.bot.controller;

import com.google.common.collect.ImmutableMap;
import com.tilitili.bot.entity.WebControlDataVO;
import com.tilitili.bot.service.MusicService;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.*;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.entity.query.BotUserSenderMappingQuery;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.query.PlayerMusicQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.manager.PlayerMusicManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.HttpClientUtil;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Controller
@RequestMapping("/api/music")
public class MusicController extends BaseController{
	private final PlayerMusicListMapper playerMusicListMapper;
	private final BotUserSenderMappingMapper botUserSenderMappingMapper;
	private final BotSenderCacheManager botSenderCacheManager;
	private final RedisCache redisCache;
	private final MusicService musicService;
	private final BotRobotCacheManager botRobotCacheManager;
	private final BotUserManager botUserManager;
	private final PlayerMusicMapper playerMusicMapper;
	private final PlayerMusicManager playerMusicManager;

	public MusicController(PlayerMusicListMapper playerMusicListMapper, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderCacheManager botSenderCacheManager, RedisCache redisCache, MusicService musicService, BotRobotCacheManager botRobotCacheManager, BotUserManager botUserManager, PlayerMusicMapper playerMusicMapper, PlayerMusicManager playerMusicManager) {
		this.playerMusicListMapper = playerMusicListMapper;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.redisCache = redisCache;
		this.musicService = musicService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.botUserManager = botUserManager;
		this.playerMusicMapper = playerMusicMapper;
		this.playerMusicManager = playerMusicManager;
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

	@GetMapping("/fileUrl")
	@ResponseBody
	public BaseModel<String> getFileUrl(@SessionAttribute(value = "userId") Long userId, Long musicId) {
		PlayerMusic playerMusic = playerMusicMapper.getPlayerMusicById(musicId);
		Asserts.notNull(playerMusic, "参数异常");
		Asserts.checkEquals(playerMusic.getUserId(), userId, "参数异常");

		List<BotUserSenderMapping> firstSenderMapping = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setUserId(userId).setPageSize(1).setPageNo(1));
		Asserts.isFalse(firstSenderMapping.isEmpty());
		BotSender firstSender = botSenderCacheManager.getValidBotSenderById(firstSenderMapping.get(0).getSenderId());

		return BaseModel.success(playerMusicManager.getFileUrl(firstSender.getBot(), new PlayerMusicDTO(playerMusic)));
	}

	@GetMapping("/fileStream")
	public ResponseEntity<InputStreamResource> getFileStream(@SessionAttribute(value = "userId") Long userId, Long musicId) throws IOException {
		PlayerMusic playerMusic = playerMusicMapper.getPlayerMusicById(musicId);
		Asserts.notNull(playerMusic, "参数异常");
		Asserts.checkEquals(playerMusic.getUserId(), userId, "参数异常");

		List<BotUserSenderMapping> firstSenderMapping = botUserSenderMappingMapper.getBotUserSenderMappingByCondition(new BotUserSenderMappingQuery().setUserId(userId).setPageSize(1).setPageNo(1));
		Asserts.isFalse(firstSenderMapping.isEmpty());
		BotSender firstSender = botSenderCacheManager.getValidBotSenderById(firstSenderMapping.get(0).getSenderId());

		String url = playerMusicManager.getFileUrl(firstSender.getBot(), new PlayerMusicDTO(playerMusic));
		Map<String, String> headers = playerMusicManager.getHeaders(playerMusic);
		InputStream audioStream = HttpClientUtil.downloadSteam(url, headers);
		log.info("url={} headers={}", url, headers);
		Asserts.notNull(audioStream, "下载文件失败");
//		Asserts.isTrue(audioStream.available() > 0, "下载文件异常");

		HttpHeaders respHeader = new HttpHeaders();
		respHeader.setContentType(MediaType.parseMediaType("audio/mp3"));
		return new ResponseEntity<>(new InputStreamResource(audioStream), respHeader, HttpStatus.OK);
	}

	public static void main(String[] args) throws IOException {
		Path tempFile = Files.createTempFile("file-", ".mp3");
		InputStream inputStream = HttpClientUtil.downloadSteam("https://music.163.com/song/media/outer/url?sc=wmv&id=558754068",
				ImmutableMap.of("User-Agent", HttpClientUtil.defaultUserAgent, "Referer", "https://music.163.com/"));
		Files.deleteIfExists(tempFile);
		Files.copy(inputStream, tempFile);
		System.out.println(tempFile.toAbsolutePath());
	}





	@GetMapping("/player")
	@ResponseBody
	public BaseModel<WebControlDataVO> getPlayerData(@SessionAttribute(value = "userId") Long userId) {
		List<BotUserSenderMapping> mappingList = botUserSenderMappingMapper.listOnlineMappingBySendTypeAndUserId(SendTypeEnum.KOOK_MESSAGE_STR, userId);
		BotUserSenderMapping mapping = mappingList.stream().findFirst().orElse(null);
		Asserts.notNull(mapping, "你好像还没加入语音");
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(mapping.getSenderId());

		String eventToken = UUID.randomUUID().toString();
		redisCache.setValue("MusicController.eventToken-"+eventToken, "yes", 10);

		WebControlDataVO response = new WebControlDataVO();
		response.setBotId(botSender.getBot());
		response.setSenderId(botSender.getId());
		response.setEventToken(eventToken);
		return BaseModel.success(response);
	}

	@PostMapping("/player/stop")
	@ResponseBody
	public BaseModel<?> stop(@RequestBody WebControlDataVO data, @SessionAttribute(value = "userId") Long userId) {
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(data.getSenderId());
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(data.getBotId());
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
		musicService.stopMusic(bot, botSender, botUser);
		return BaseModel.success();
	}

	@PostMapping("/player/start")
	@ResponseBody
	public BaseModel<?> start(@RequestBody WebControlDataVO data, @SessionAttribute(value = "userId") Long userId) {
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(data.getSenderId());
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(data.getBotId());
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
		musicService.startMusic(bot, botSender, botUser);
		return BaseModel.success();
	}

	@PostMapping("/player/last")
	@ResponseBody
	public BaseModel<?> last(@RequestBody WebControlDataVO data, @SessionAttribute(value = "userId") Long userId) {
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(data.getSenderId());
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(data.getBotId());
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
		musicService.lastMusic(bot, botSender, botUser);
		return BaseModel.success();
	}

	@PostMapping("/player/list/start")
	@ResponseBody
	public BaseModel<?> startList(@RequestBody WebControlDataVO data, @SessionAttribute(value = "userId") Long userId) {
		BotSender botSender = botSenderCacheManager.getValidBotSenderById(data.getSenderId());
		BotRobot bot = botRobotCacheManager.getValidBotRobotById(data.getBotId());
		BotUserDTO botUser = botUserManager.getValidBotUserByIdWithParent(userId);
		musicService.startList(bot, botSender, botUser);
		return BaseModel.success();
	}
}
