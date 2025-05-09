package com.tilitili.bot.service;

import com.tilitili.bot.BotApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
public class PixivCacheServiceTest {
    @Autowired
    private PixivCacheService tester;

    @Test
    public void findImage() {
        System.out.println(tester.findImage("https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2219218480-87DB903784FC358C5C0BE9933712EC9D/0?term"));
    }
}