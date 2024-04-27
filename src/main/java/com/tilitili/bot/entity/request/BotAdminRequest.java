package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class BotAdminRequest extends BaseDTO {
    private String password;
    private String email;
    private String code;
    private String emailCode;
    private String master;
    private Boolean remember;

    public String getCode() {
        return code;
    }

    public BotAdminRequest setCode(String code) {
        this.code = code;
        return this;
    }

    public String getEmailCode() {
        return emailCode;
    }

    public BotAdminRequest setEmailCode(String emailCode) {
        this.emailCode = emailCode;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public BotAdminRequest setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public BotAdminRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getMaster() {
        return master;
    }

    public BotAdminRequest setMaster(String master) {
        this.master = master;
        return this;
    }

    public Boolean getRemember() {
        return remember;
    }

    public BotAdminRequest setRemember(Boolean remember) {
        this.remember = remember;
        return this;
    }
}
