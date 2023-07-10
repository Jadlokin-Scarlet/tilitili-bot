package com.tilitili.bot.entity.request;

public class ReportCookieRequest {
    private String key;
    private String cookie;

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
}
