package com.tilitili.bot.service;

import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class BotSessionService {
    private final Map<String, MiraiSession> sessionMap;
    private final RedisCache redisCache;

    @Autowired
    public BotSessionService(RedisCache redisCache) {
        this.redisCache = redisCache;
        this.sessionMap = new HashMap<>();
    }

    public String getSessionKey(Long senderId) {
        return "sessionKey-"+senderId;
    }

    public MiraiSession getSession(Long senderId) {
        return getSession(this.getSessionKey(senderId));
    }

    public MiraiSession getSession(String sessionKey) {
        if (! sessionMap.containsKey(sessionKey)) {
            sessionMap.put(sessionKey, new MiraiSession(sessionKey));
        }
        return sessionMap.get(sessionKey);
    }

    public class MiraiSession {
        private final String sessionKey;
        public MiraiSession(String sessionKey) {
            this.sessionKey = sessionKey;
        }
        private String generateKey(String key) {
            return sessionKey + "-" + key;
        }
        public String get(String key) {
            return redisCache.getValueString(this.generateKey(key));
        }
        public String getOrDefault(String key, String or) {
            if (redisCache.exists(this.generateKey(key))) {
                return this.get(key);
            } else {
                return or;
            }
        }
        public Long getLong(String key) {
            return redisCache.getValueLong(this.generateKey(key));
        }
        public void put(String key, Object value) {
            redisCache.setValue(this.generateKey(key), value);
        }
        public void put(String key, String value, int delay) {
            redisCache.setValue(this.generateKey(key), value, delay);
        }
        public Boolean putIfAbsent(String key, String value, Duration expireTime) {
            return redisCache.putIfAbsent(this.generateKey(key), value, expireTime);
        }
        public Boolean tryTimeoutLock(String key, Duration expireTime) {
            return redisCache.tryTimeoutLock(this.generateKey(key), expireTime);
        }
        public boolean containsKey(String key) {
            return redisCache.exists(this.generateKey(key));
        }
        public Boolean remove(String key) {
            return redisCache.delete(this.generateKey(key));
        }

	}
}
