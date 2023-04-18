
package com.tilitili.bot.socket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ShortUrlWebSocketHandler extends WebSocketClient implements ApplicationListener<ContextClosedEvent> {
    private static final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
    private final RedisCache redisCache;
    private AtomicInteger status = new AtomicInteger(0);

    public ShortUrlWebSocketHandler(URI serverUri, RedisCache redisCache) {
        super(serverUri);
        this.redisCache = redisCache;
    }

    @Override
    public void onMessage(String message) {
        try {
            log.info(message);
            if (message.startsWith("0")) {
                this.send("40/socket.io.xmsl?guest=nFFA8rxHbCmdi8TN,");
            } else if (Objects.equals(message, "40/socket.io.xmsl")) {
                status.set(2);
                this.send("2");
            } else if (message.equals("3")) {
                scheduled.schedule(() -> this.send("2"),  30, TimeUnit.SECONDS);
            } else if (message.contains("create_link")) {
                String json = message.replace("42/socket.io.xmsl,", "");
                JSONArray resp = JSONObject.parseArray(json);
                JSONObject data = resp.getJSONObject(1).getJSONObject("data");
                String shortUrl = data.getString("url");
                String originUrl = data.getString("origin_url");
                redisCache.setValue(originUrl, shortUrl, 60 * 29);
            }
        } catch (AssertException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getShortUrl(String url) {
        if (status.compareAndSet(0, 1)) {
            this.reconnect();
        }
        if (!this.waitStart()) {
            return url;
        }
        this.send("42/socket.io.xmsl,[\"create_link\",{\"origin_url\":\""+url+"\"}]");
        // 异步等待结果
        long start = System.currentTimeMillis();
        try {
            while (true) {
                String shortUrl = (String) redisCache.getValue(url);
                if (shortUrl != null) {
                    return shortUrl;
                }
                //如果大于1200ms直接返回
                if (System.currentTimeMillis() - start > 30000) {
                    return null;
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            log.error("等待短连接异常", e);
            return null;
        }
    }

    private boolean waitStart() {
        // 异步等待结果
        long start = System.currentTimeMillis();
        try {
            while (true) {
                if (status.get() == 2) {
                    return true;
                }
                //如果大于1200ms直接返回
                if (System.currentTimeMillis() - start > 30000) {
                    return false;
                } else {
                    TimeUtil.millisecondsSleep(100);
                }
            }
        } catch (Exception e) {
            log.error("等待短连接异常", e);
            return false;
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("连接websocket成功，url={}", getURI().toString());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error("连接关闭，url={} code ={}, reason={}, remote={}", this.uri.toString(), code, reason, remote);
        status.set(0);
    }

    @Override
    public void onError(Exception ex) {
        log.error("websocket异常", ex);
        status.set(0);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        try {
            this.closeBlocking();
        } catch (InterruptedException e) {
            log.error("优雅停机异常");
        }
    }
}
