package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class PixivCacheServiceTest {
    @Autowired
    private PixivCacheService tester;

    @Test
    public void findImage() {
        System.out.println(tester.findImage("https://gchat.qpic.cn/gchatpic_new/1701008067/756782332-2382760790-2AC03AC7A1713CEB1674BB6793FB17E8/0?term=2&amp"));
    }
}