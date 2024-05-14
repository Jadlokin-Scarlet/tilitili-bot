package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.bot.entity.response.ListPlayerMusicResponse;
import com.tilitili.common.component.music.MusicQueueFactory;
import com.tilitili.common.component.music.MusicRedisQueue;
import com.tilitili.common.entity.dto.PlayerMusicDTO;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.JmsTemplateFactory;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/api/pub/music")
public class MusicPubController extends BaseController {
	private final RedisCache redisCache;
	private final Map<Long, List<SseEmitter>> emitterMap = new HashMap<>();

	public MusicPubController(RedisCache redisCache) {
		this.redisCache = redisCache;
	}

	@GetMapping("/player/{botId}/event")
	public SseEmitter registerSubscribe(@PathVariable Long botId, @RequestParam String eventToken) {
		Asserts.isTrue(redisCache.delete("MusicController.eventToken-"+eventToken), "参数异常");
		log.info("new sseEmitter");
		SseEmitter sseEmitter = new SseEmitter(-1L);
		emitterMap.computeIfAbsent(botId, key -> new LinkedList<>()).add(sseEmitter);

		sseEmitter.onCompletion(()-> {
			log.info("completion");
			emitterMap.get(botId).remove(sseEmitter);
		});
		sseEmitter.onTimeout(()-> {
			log.info("timeout");
			emitterMap.get(botId).remove(sseEmitter);
		});
		sseEmitter.onError((e) -> {
			log.warn("error", e);
			emitterMap.get(botId).remove(sseEmitter);
		});

//		Executors.newScheduledThreadPool(10).scheduleWithFixedDelay(() -> {
		ktvUpdateMessage(botId);
//		}, 0, 10, TimeUnit.SECONDS);

		return sseEmitter;
	}

	@GetMapping("/player/{botId}/event2")
	public SseEmitter registerSubscribe(@PathVariable Long botId) {
		log.info("new sseEmitter");
		SseEmitter sseEmitter = new SseEmitter(-1L);
		emitterMap.computeIfAbsent(botId, key -> new LinkedList<>()).add(sseEmitter);

		sseEmitter.onCompletion(()-> {
			log.info("completion");
			emitterMap.get(botId).remove(sseEmitter);
		});
		sseEmitter.onTimeout(()-> {
			log.info("timeout");
			emitterMap.get(botId).remove(sseEmitter);
		});
		sseEmitter.onError((e) -> {
			log.warn("error", e);
			emitterMap.get(botId).remove(sseEmitter);
		});

//		Executors.newScheduledThreadPool(10).scheduleWithFixedDelay(() -> {
		ktvUpdateMessage(botId);
//		}, 0, 10, TimeUnit.SECONDS);

		return sseEmitter;
	}

	@JmsListener(destination = JmsTemplateFactory.KEY_KTV_UPDATE, containerFactory = "topicFactory")
	public void ktvUpdateMessage(Long botId) {
		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(botId, redisCache);
		PlayerMusicDTO theMusic = musicRedisQueue.getTheMusic();
		ListPlayerMusicResponse response = new ListPlayerMusicResponse();
		response.setTheMusic(theMusic);
		response.setBotId(botId);

		List<SseEmitter> emitterList = this.emitterMap.getOrDefault(botId, Collections.emptyList());
		log.info("send ktv update to botId={} size={}", botId, emitterList.size());
		for (Iterator<SseEmitter> iterator = emitterList.iterator(); iterator.hasNext(); ) {
			SseEmitter emitter = iterator.next();
			try {
				emitter.send(SseEmitter.event().name("ktvUpdate").data(response));
			} catch (Exception e) {
				iterator.remove();
				log.warn("下发事件异常，移除emitter", e);
			}
		}
//		log.info("send ktv update end");
	}

}
