package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotFunctionTalk;

public class BotFunctionTalkDTO extends BotFunctionTalk {
    private String senderName;
    private String sendType;

    public String getSenderName() {
        return senderName;
    }

    public BotFunctionTalkDTO setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public String getSendType() {
        return sendType;
    }

    public BotFunctionTalkDTO setSendType(String sendType) {
        this.sendType = sendType;
        return this;
    }
}
