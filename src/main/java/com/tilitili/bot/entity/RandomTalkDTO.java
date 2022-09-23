package com.tilitili.bot.entity;

import com.tilitili.bot.annotation.ExcelProperty;

public class RandomTalkDTO {
	@ExcelProperty(value = "群号")
	private Long group;
	@ExcelProperty(value = "关键词")
	private String req;
	@ExcelProperty(value = "回复")
	private String resp;

	public Long getGroup() {
		return group;
	}

	public RandomTalkDTO setGroup(Long group) {
		this.group = group;
		return this;
	}

	public String getReq() {
		return req;
	}

	public RandomTalkDTO setReq(String req) {
		this.req = req;
		return this;
	}

	public String getResp() {
		return resp;
	}

	public RandomTalkDTO setResp(String resp) {
		this.resp = resp;
		return this;
	}
}
