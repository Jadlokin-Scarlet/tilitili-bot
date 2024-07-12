package com.tilitili.bot.component;

import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class MessageFolder {
	private final RedisCache redisCache;

	public MessageFolder(RedisCache redisCache) {
		this.redisCache = redisCache;
	}

	public void addMessage(String key) {
		redisCache.increment(key);
		redisCache.putIfAbsent("MessageFolder.lock-"+key, true, Duration.ofSeconds(1));
	}

	public int waitAndGetMessageCnt(String key) {
//		redisCache.tryTimeoutWait()
		return 0;
	}
}
