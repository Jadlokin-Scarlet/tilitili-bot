package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotRobotSenderMapping;

public class BotRobotSenderMappingDTO extends BotRobotSenderMapping {
    private String senderName;
    private String botName;
    private String type;

    public String getType() {
        return type;
    }

    public BotRobotSenderMappingDTO setType(String type) {
        this.type = type;
        return this;
    }

    public String getSenderName() {
        return senderName;
    }

    public BotRobotSenderMappingDTO setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public String getBotName() {
        return botName;
    }

    public BotRobotSenderMappingDTO setBotName(String botName) {
        this.botName = botName;
        return this;
    }
}
