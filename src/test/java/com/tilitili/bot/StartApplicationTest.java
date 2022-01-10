package com.tilitili.bot;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tilitili.bot.emnus.MessageHandleEnum;
import com.tilitili.bot.service.mirai.HelpHandle;
import com.tilitili.common.entity.view.bot.gocqhttp.GoCqhttpWsMessage;
import com.tilitili.common.utils.RedisCache;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
class StartApplicationTest {
    @Autowired
    private HelpHandle helpHandle;
    @Autowired
    private RedisCache redisCache;

    @Test
    void main() {
        Long start = redisCache.increment("testMap", "test");
        System.out.println(start);
    }
}