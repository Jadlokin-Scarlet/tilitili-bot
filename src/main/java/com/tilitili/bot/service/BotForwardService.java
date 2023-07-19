package com.tilitili.bot.service;

import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.mapper.mysql.BotForwardConfigMapper;
import com.tilitili.common.mapper.mysql.BotSenderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BotForwardService {
    private final BotForwardConfigMapper botForwardConfigMapper;
    private final BotSenderMapper botSenderMapper;
    private final BotRobotCacheManager botRobotCacheManager;

    public BotForwardService(BotForwardConfigMapper botForwardConfigMapper, BotSenderMapper botSenderMapper, BotRobotCacheManager botRobotCacheManager) {
        this.botForwardConfigMapper = botForwardConfigMapper;
        this.botSenderMapper = botSenderMapper;
        this.botRobotCacheManager = botRobotCacheManager;
    }

    public BaseModel<PageModel<BotForwardConfig>> listForwardConfig(BotForwardConfigQuery query) {
        BotRobot bot = botRobotCacheManager.getValidBotRobotById(query.getBotId());

        return null;
    }
}
