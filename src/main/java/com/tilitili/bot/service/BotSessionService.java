package com.tilitili.bot.service;

import com.tilitili.common.emnus.SendTypeEmum;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.Asserts;
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

    public String getSessionKey(BotMessage botMessage) {
        BotSender botSender = botMessage.getBotSender();
        String sendType = botSender.getSendType();
        Long qq = botSender.getQq();
        Long group = botSender.getGroup();
        Long guildId = botSender.getGuildId();
        Long channelId = botSender.getChannelId();
        return getSessionKey(sendType, qq, group, guildId, channelId);
    }

    public String getSessionKey(BotSender botSender) {
        String sendType = botSender.getSendType();
        Long qq = botSender.getQq();
        Long group = botSender.getGroup();
        Long guildId = botSender.getGuildId();
        Long channelId = botSender.getChannelId();
        return getSessionKey(sendType, qq, group, guildId, channelId);
    }

    private String getSessionKey(String sendType, Long qq, Long group, Long guildId, Long channelId) {
        switch (sendType) {
            case SendTypeEmum.TEMP_MESSAGE_STR: Asserts.notNull(group, "找不到发送对象");
            case SendTypeEmum.FRIEND_MESSAGE_STR: Asserts.notNull(qq, "找不到发送对象");return SendTypeEmum.FRIEND_MESSAGE_STR + "-" + qq;
            case SendTypeEmum.GROUP_MESSAGE_STR: Asserts.notNull(group, "找不到发送对象");return SendTypeEmum.GROUP_MESSAGE_STR + "-" + group;
            case SendTypeEmum.GUILD_MESSAGE_STR: {
                Asserts.notNull(guildId, "找不到发送对象");
                Asserts.notNull(channelId, "找不到发送对象");
                return SendTypeEmum.GUILD_MESSAGE_STR + "-" + guildId + "-" + channelId;
            }
            default: throw new AssertException("未知发送类型：" + sendType);
        }
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
