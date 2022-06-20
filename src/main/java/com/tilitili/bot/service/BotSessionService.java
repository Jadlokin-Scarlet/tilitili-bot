package com.tilitili.bot.service;

import com.tilitili.common.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        public String getSessionKey() {
            return sessionKey;
        }
        public String get(String key) {
            return (String) redisCache.getMapValue(sessionKey, key);
        }
        public String getOrDefault(String key, String or) {
            if (redisCache.existsHashKey(sessionKey, key)) {
                return (String) redisCache.getMapValue(sessionKey, key);
            } else {
                return or;
            }
        }
        public void put(String key, String value) {
            redisCache.addMapValue(sessionKey, key, value);
        }
        public boolean containsKey(String key) {
            return redisCache.existsHashKey(sessionKey, key);
        }
        public Object remove(String key) {
            return redisCache.removeMapValue(sessionKey, key);
        }
    }
}
