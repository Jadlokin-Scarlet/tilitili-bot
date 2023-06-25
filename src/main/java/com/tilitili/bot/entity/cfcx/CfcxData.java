package com.tilitili.bot.entity.cfcx;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class CfcxData extends BaseDTO {
    private Integer total;
    private List<CfcxItem> list;

    public Integer getTotal() {
        return total;
    }

    public CfcxData setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public List<CfcxItem> getList() {
        return list;
    }

    public CfcxData setList(List<CfcxItem> list) {
        this.list = list;
        return this;
    }
}
