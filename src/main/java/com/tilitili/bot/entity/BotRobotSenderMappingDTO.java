package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotRobotSenderMapping;

public class BotRobotSenderMappingDTO extends BotRobotSenderMapping {
    private String name;
    private String type;

    public String getType() {
        return type;
    }

    public BotRobotSenderMappingDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public BotRobotSenderMappingDTO setName(String name) {
        this.name = name;
        return this;
    }
}
