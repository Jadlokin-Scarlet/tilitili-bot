package com.tilitili.bot.service;

import com.tilitili.common.manager.GoCqhttpManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoCqhttpService {
    private final GoCqhttpManager goCqhttpManager;

    @Autowired
    public GoCqhttpService(GoCqhttpManager goCqhttpManager) {
        this.goCqhttpManager = goCqhttpManager;
    }

    public void syncHandleTextMessage(String messageStr) {
        log.info(messageStr);
    }
}
