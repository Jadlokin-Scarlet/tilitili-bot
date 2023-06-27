package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.BotMenuMapping;

public class UpdateRoleMappingRequest extends BotMenuMapping {
    private Boolean checked;

    public Boolean getChecked() {
        return checked;
    }

    public UpdateRoleMappingRequest setChecked(Boolean checked) {
        this.checked = checked;
        return this;
    }
}
