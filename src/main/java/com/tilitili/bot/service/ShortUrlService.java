package com.tilitili.bot.service;

import com.tilitili.bot.socket.ShortUrlWebSocketHandler;
import com.tilitili.common.utils.RedisCache;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ShortUrlService {
    private final ShortUrlWebSocketHandler webSocketHandler;

    public ShortUrlService(RedisCache redisCache) throws URISyntaxException {
        webSocketHandler = new ShortUrlWebSocketHandler(new URI("wss://sl.xiaomark.com/socket.io/?guest=nFFA8rxHbCmdi8TN&EIO=3&transport=websocket"), redisCache);
//        webSocketHandler.connect();
    }

    public String getShortUrl(String url) {
        String shortUrl = webSocketHandler.getShortUrl(url);
        if (shortUrl == null) {
            return url;
        } else {
            return shortUrl;
        }
    }

}
