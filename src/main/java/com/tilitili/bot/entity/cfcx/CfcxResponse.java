package com.tilitili.bot.entity.cfcx;

public class CfcxResponse {
    private Integer code;
    private String message;
    private CfcxData data;

    public Integer getCode() {
        return code;
    }

    public CfcxResponse setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public CfcxResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public CfcxData getData() {
        return data;
    }

    public CfcxResponse setData(CfcxData data) {
        this.data = data;
        return this;
    }
}
