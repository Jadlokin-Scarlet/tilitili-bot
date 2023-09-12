package com.tilitili.bot.entity.mirai;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class TestRequest extends BaseDTO {
    List<String> title;

    public List<String> getTitle() {
        return title;
    }

    public TestRequest setTitle(List<String> title) {
        this.title = title;
        return this;
    }
}
