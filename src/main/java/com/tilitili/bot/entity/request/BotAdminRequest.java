package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class BotAdminRequest extends BaseDTO {
    private String username;
    private String password;
    private String email;
    private String code;
    private String emailCode;
    private Long masterQQ;

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

    public String getUsername() {
        return username;
    }

    public BotAdminRequest setUsername(String username) {
        this.username = username;
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

    public Long getMasterQQ() {
        return masterQQ;
    }

    public BotAdminRequest setMasterQQ(Long masterQQ) {
        this.masterQQ = masterQQ;
        return this;
    }
}
