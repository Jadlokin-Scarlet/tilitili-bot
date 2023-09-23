package com.tilitili.bot.service.mirai;

import com.tilitili.bot.StartApplication;
import com.tilitili.bot.util.BotMessageActionUtil;
import com.tilitili.common.entity.view.bot.BotMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = StartApplication.class)
class FF14HandleTest {
    @Autowired
    private FF14Handle tester;

    @Test
    void handleMessage() throws Exception {
        BotMessage botMessage = tester.handleMessage(BotMessageActionUtil.buildEmptyAction("ff 青鸟"));
        System.out.println(botMessage);
    }
}