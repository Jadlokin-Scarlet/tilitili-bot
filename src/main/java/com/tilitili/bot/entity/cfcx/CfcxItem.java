package com.tilitili.bot.entity.cfcx;

public class CfcxItem {
    private Integer attribute;
    private String face;
    private Integer mid;
    private String sign;
    private Integer special;
    private String uname;

    public Integer getAttribute() {
        return attribute;
    }

    public CfcxItem setAttribute(Integer attribute) {
        this.attribute = attribute;
        return this;
    }

    public String getFace() {
        return face;
    }

    public CfcxItem setFace(String face) {
        this.face = face;
        return this;
    }

    public Integer getMid() {
        return mid;
    }

    public CfcxItem setMid(Integer mid) {
        this.mid = mid;
        return this;
    }

    public String getSign() {
        return sign;
    }

    public CfcxItem setSign(String sign) {
        this.sign = sign;
        return this;
    }

    public Integer getSpecial() {
        return special;
    }

    public CfcxItem setSpecial(Integer special) {
        this.special = special;
        return this;
    }

    public String getUname() {
        return uname;
    }

    public CfcxItem setUname(String uname) {
        this.uname = uname;
        return this;
    }
}
