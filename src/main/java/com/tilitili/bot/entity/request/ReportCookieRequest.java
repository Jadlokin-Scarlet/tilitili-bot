package com.tilitili.bot.entity.request;

public class ReportCookieRequest {
    private String key;
    private String cookie;
    private String code;
    private Long userId;

    public String getKey() {
        return key;
    }

    public ReportCookieRequest setKey(String key) {
        this.key = key;
        return this;
    }

    public String getCookie() {
        return cookie;
    }

    public ReportCookieRequest setCookie(String cookie) {
        this.cookie = cookie;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ReportCookieRequest setCode(String code) {
        this.code = code;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public ReportCookieRequest setUserId(Long userId) {
        this.userId = userId;
        return this;
    }
}
