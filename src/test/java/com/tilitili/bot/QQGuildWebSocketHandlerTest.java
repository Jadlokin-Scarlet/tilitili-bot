package com.tilitili.bot;

import com.tilitili.bot.socket.QQGuildWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class QQGuildWebSocketHandlerTest {
    QQGuildWebSocketHandler shortUrlService;

    @Test
    public void test() throws URISyntaxException {
        QQGuildWebSocketHandler webSocketHandler = new QQGuildWebSocketHandler(
                new URI("wss://sandbox.api.sgroup.qq.com/websocket"),
                null, null, null
                );
        webSocketHandler.connect();

    }
}