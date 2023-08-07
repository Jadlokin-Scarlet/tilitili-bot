package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotConfig;

import java.util.List;

public class BotConfigUpdateRequest {
    private List<BotConfig> configList;

    public List<BotConfig> getConfigList() {
        return configList;
    }

    public BotConfigUpdateRequest setConfigList(List<BotConfig> configList) {
        this.configList = configList;
        return this;
    }
}
