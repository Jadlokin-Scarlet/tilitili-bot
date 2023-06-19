package com.tilitili.bot.service;

import com.tilitili.common.exception.AssertException;
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
        public String getSessionKey() {
            return sessionKey;
        }
        public String get(String key) {
            Object value = redisCache.getMapValue(sessionKey, key);
            if (value == null) {
                return null;
            }
            if (!(value instanceof String)) {
                throw new AssertException();
            }
            return (String) value;
        }
        public String getOrDefault(String key, String or) {
            if (redisCache.existsHashKey(sessionKey, key)) {
                return this.get(key);
            } else {
                return or;
            }
        }
        public Long getLong(String key) {
            Object value = redisCache.getMapValue(sessionKey, key);
            if (value == null) {
                return null;
            }
            if (value instanceof Integer) {
                return Long.valueOf(((Integer) value));
            } else if (value instanceof Long) {
                return (Long) value;
            } else {
                throw new AssertException();
            }
        }
        public Integer getInteger(String key) {
            Object value = redisCache.getMapValue(sessionKey, key);
            if (value == null) {
                return null;
            }
            if (value instanceof Integer) {
                return ((Integer) value);
            } else {
                throw new AssertException();
            }
        }
        public void put(String key, Object value) {
            redisCache.addMapValue(sessionKey, key, value);
        }
        public Boolean putIfAbsent(String key, String value) {
            return redisCache.addIfAbsentMapValue(sessionKey, key, value);
        }
        public boolean containsKey(String key) {
            return redisCache.existsHashKey(sessionKey, key);
        }
        public Object remove(String key) {
            return redisCache.removeMapValue(sessionKey, key);
        }
    }
}
