
package com.tilitili.bot.socket;

import com.google.common.collect.ImmutableMap;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WendaWebSocketHandler extends BaseWebSocketHandler implements ApplicationListener<ContextClosedEvent> {
    private static final ScheduledExecutorService scheduled =  Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean lockFlag = new AtomicBoolean(false);
    private final RedisCache redisCache;

    public WendaWebSocketHandler(URI serverUri, RedisCache redisCache) {
        super(serverUri);
        this.redisCache = redisCache;
    }

    @Override
    public void handleTextMessage(String message) {
        redisCache.setValue("WendaWebSocketHandler.resp", message, 60 * 29);
    }

    public String chat(String originReq, String req, Double temperature, Double topP) {
        try {
            Asserts.isTrue(lockFlag.compareAndSet(false, true), "猪脑过载，你先别急Σ（ﾟдﾟlll）");
            String message = Gsons.toJson(ImmutableMap.of(
                    "prompt", req,
                    "keyword", originReq,
                    "temperature", temperature,
                    "top_p", topP,
                    "max_length", 4096,
                    "history", Collections.emptyList()));
            this.send(message);
            // 异步等待结果
            long start = System.currentTimeMillis();

            while (true) {
                String resp = (String) redisCache.getValue("WendaWebSocketHandler.resp");
                if (resp != null) {
                    return resp;
                }
                //如果大于1200ms直接返回
                if (System.currentTimeMillis() - start > 30000) {
                    return null;
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            log.error("等待响应异常", e);
            return null;
        } finally {
            lockFlag.set(false);
        }
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
