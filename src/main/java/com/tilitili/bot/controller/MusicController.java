package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.entity.response.ListPlayerMusicResponse;
import com.tilitili.bot.service.MusicService;
import com.tilitili.common.component.music.MusicQueueFactory;
import com.tilitili.common.component.music.MusicRedisQueue;
import com.tilitili.common.emnus.SendTypeEnum;
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
import com.tilitili.common.utils.JmsTemplateFactory;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
	private final List<SseEmitter> emitterList = new ArrayList<>();

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
		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(botSender.getBot(), redisCache);
		PlayerMusicDTO theMusic = musicRedisQueue.getTheMusic();
		ListPlayerMusicResponse response = new ListPlayerMusicResponse();
		response.setTheMusic(theMusic);
		return BaseModel.success(response);
	}

	@GetMapping("/player/event")
	public ResponseEntity<SseEmitter> registerSubscribe() {
		log.info("new sseEmitter");
		SseEmitter sseEmitter = new SseEmitter(-1L);
		emitterList.add(sseEmitter);

		sseEmitter.onCompletion(()-> {
			log.info("completion");
			emitterList.remove(sseEmitter);
		});
		sseEmitter.onTimeout(()-> {
			log.info("timeout");
			emitterList.remove(sseEmitter);
		});
		sseEmitter.onError((e) -> {
			log.warn("error", e);
			emitterList.remove(sseEmitter);
		});

		return new ResponseEntity<>(sseEmitter, HttpStatus.OK);
	}

	private final ScheduledExecutorService scheduled =  Executors.newScheduledThreadPool(10);

	@JmsListener(destination = JmsTemplateFactory.KEY_KTV_UPDATE, containerFactory = "topicFactory")
	public void ktvUpdateMessage(Long botId) {
		log.info("send to emitter botId={} size={}", botId, emitterList.size());
		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(botId, redisCache);
		PlayerMusicDTO theMusic = musicRedisQueue.getTheMusic();
		ListPlayerMusicResponse response = new ListPlayerMusicResponse();
		response.setTheMusic(theMusic);

		for (Iterator<SseEmitter> iterator = this.emitterList.iterator(); iterator.hasNext(); ) {
			SseEmitter emitter = iterator.next();
			try {
				emitter.send(SseEmitter.event().name("ktvUpdate").data(response));
			} catch (Exception e) {
				log.warn("下发事件异常，移除emitter", e);
				iterator.remove();
			}
		}
	}

}
