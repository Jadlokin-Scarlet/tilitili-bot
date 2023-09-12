package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
public class PixivCacheServiceTest {
    @Autowired
    private PixivCacheService tester;

    @Test
    public void findImage() {
        System.out.println(tester.findImage("https://gchat.qpic.cn/qmeetpic/49134681639135681/43227251-2219218480-87DB903784FC358C5C0BE9933712EC9D/0?term"));
    }
}