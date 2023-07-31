package com.tilitili.bot.entity.request;

public class ReportCookieRequest {
    private String key;
    private String cookie;
    private String adminCode;

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

    public String getAdminCode() {
        return adminCode;
    }

    public ReportCookieRequest setAdminCode(String adminCode) {
        this.adminCode = adminCode;
        return this;
    }
}
