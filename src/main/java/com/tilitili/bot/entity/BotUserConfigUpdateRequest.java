package com.tilitili.bot.entity;

import com.tilitili.common.entity.BotUserConfig;

import java.util.List;

public class BotUserConfigUpdateRequest {
    private List<BotUserConfig> configList;

    public List<BotUserConfig> getConfigList() {
        return configList;
    }

    public BotUserConfigUpdateRequest setConfigList(List<BotUserConfig> configList) {
        this.configList = configList;
        return this;
    }
}
