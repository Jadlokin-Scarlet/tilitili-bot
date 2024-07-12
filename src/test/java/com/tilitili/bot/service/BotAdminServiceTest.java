package com.tilitili.bot.service;

import com.tilitili.bot.BotApplication;
import com.tilitili.bot.entity.request.BotAdminRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = BotApplication.class)
public class BotAdminServiceTest {
    @Autowired
    private BotAdminService tester;

    @Test
    public void sendEmailCode() {
        tester.sendEmailCode(new BotAdminRequest().setCode("GEGASJ").setEmail("545459363@qq.com"));
    }
}