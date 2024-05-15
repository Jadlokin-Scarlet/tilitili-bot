package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.bot.entity.WebControlDataVO;
import com.tilitili.common.component.music.MusicQueueFactory;
import com.tilitili.common.component.music.MusicRedisQueue;
import com.tilitili.common.entity.dto.KtvEvent;
import com.tilitili.common.utils.JmsTemplateFactory;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public SseEmitter registerSubscribe(@PathVariable Long botId) {
//		Asserts.isTrue(redisCache.delete("MusicController.eventToken-"+eventToken), "参数异常");
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

		ktvUpdateMessage(new KtvEvent().setType(KtvEvent.TYPE_UPDATE).setBotId(botId));

		return sseEmitter;
	}

	@JmsListener(destination = JmsTemplateFactory.KEY_KTV_MESSAGE, containerFactory = "topicFactory")
	public void ktvUpdateMessage(KtvEvent event) {
		Long botId = event.getBotId();
		MusicRedisQueue musicRedisQueue = MusicQueueFactory.getQueueInstance(botId, redisCache);

		WebControlDataVO response = new WebControlDataVO();
		if (KtvEvent.TYPE_UPDATE.equals(event.getType())) {
			response.setTheMusic(musicRedisQueue.getTheMusic());
			response.setPlayerQueue(musicRedisQueue.getPlayerQueue());
			response.setMusicList(musicRedisQueue.getMusicList());
		}
		response.setStopFlag(event.getStop());

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
