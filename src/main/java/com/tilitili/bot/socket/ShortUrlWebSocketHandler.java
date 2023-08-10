
package com.tilitili.bot.socket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tilitili.common.exception.AssertException;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShortUrlWebSocketHandler extends BaseWebSocketHandler {
    private final RedisCache redisCache;

    public ShortUrlWebSocketHandler(RedisCache redisCache) throws URISyntaxException {
        super(new URI("wss://sl.xiaomark.com/socket.io/?guest=nFFA8rxHbCmdi8TN&EIO=3&transport=websocket"));
        this.redisCache = redisCache;
    }

    @Override
    protected void handleTextMessage(String message) {
        try {
            if (message.startsWith("0")) {
                this.send("40/socket.io.xmsl?guest=nFFA8rxHbCmdi8TN,");
            } else if (Objects.equals(message, "40/socket.io.xmsl")) {
                status.set(2);
                this.send("2");
            } else if (message.equals("3")) {
                executorService.schedule(() -> this.send("2"),  30, TimeUnit.SECONDS);
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
            executorService.schedule(this::reconnect,  1, TimeUnit.MILLISECONDS);
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
}
