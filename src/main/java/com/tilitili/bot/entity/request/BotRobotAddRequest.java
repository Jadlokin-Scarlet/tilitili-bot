package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.BotRobot;

public class BotRobotAddRequest extends BotRobot {
    private Long botId;

    public Long getBotId() {
        return botId;
    }

    public BotRobotAddRequest setBotId(Long botId) {
        this.botId = botId;
        return this;
    }
}
