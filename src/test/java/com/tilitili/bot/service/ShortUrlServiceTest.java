package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import com.tilitili.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StartApplication.class)
public class ShortUrlServiceTest {
    @Autowired
    ShortUrlService shortUrlService;

    @Test
    public void getShortUrl() {
//        shortUrlService.getShortUrl();
    }
}