package com.tilitili.bot.service;

import com.tilitili.bot.StartApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
public class ShortUrlServiceTest {
    @Autowired
    ShortUrlService shortUrlService;

    @Test
    public void getShortUrl() {
//        shortUrlService.getShortUrl();
    }
}