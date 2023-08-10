package com.tilitili.bot.service;

import org.springframework.stereotype.Service;

@Service
public class ShortUrlService {
//    private final WebSocketFactory webSocketFactory;
//
//    public ShortUrlService(WebSocketFactory webSocketFactory) {
//        this.webSocketFactory = webSocketFactory;
//    }

    public String getShortUrl(String url) {
//        ShortUrlWebSocketHandler shortUrlWebSocketHandler = webSocketFactory.getShortUrlWebSocketHandler();
//        String shortUrl = shortUrlWebSocketHandler.getShortUrl(url);
//        if (shortUrl == null) {
            return url;
//        } else {
//            return shortUrl;
//        }
    }

}
