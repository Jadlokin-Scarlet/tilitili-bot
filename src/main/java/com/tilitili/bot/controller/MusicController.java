package com.tilitili.bot.controller;

import com.tilitili.bot.entity.WebControlDataVO;
import com.tilitili.bot.service.MusicService;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.BotUserSenderMapping;
import com.tilitili.common.entity.PlayerMusicList;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.PlayerMusicListQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.manager.BotSenderCacheManager;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.mapper.mysql.BotUserSenderMappingMapper;
import com.tilitili.common.mapper.mysql.PlayerMusicListMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

	public MusicController(PlayerMusicListMapper playerMusicListMapper, BotUserSenderMappingMapper botUserSenderMappingMapper, BotSenderCacheManager botSenderCacheManager, RedisCache redisCache, MusicService musicService, BotRobotCacheManager botRobotCacheManager, BotUserManager botUserManager) {
		this.playerMusicListMapper = playerMusicListMapper;
		this.botUserSenderMappingMapper = botUserSenderMappingMapper;
		this.botSenderCacheManager = botSenderCacheManager;
		this.redisCache = redisCache;
		this.musicService = musicService;
		this.botRobotCacheManager = botRobotCacheManager;
		this.botUserManager = botUserManager;
	}

	@GetMapping("/list")
	@ResponseBody
	public BaseModel<List<PlayerMusicList>> listMusic(@SessionAttribute(value = "userId") Long userId) {
		List<PlayerMusicList> listList = playerMusicListMapper.getPlayerMusicListByCondition(new PlayerMusicListQuery().setUserId(userId));
		return BaseModel.success(listList);
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
