package com.tilitili.bot.entity;

import com.google.gson.JsonElement;
import com.tilitili.common.entity.dto.BaseDTO;

public class McPanelWsData extends BaseDTO {
	private JsonElement data;
	private String type;

	public JsonElement getData() {
		return data;
	}

	public McPanelWsData setData(JsonElement data) {
		this.data = data;
		return this;
	}

	public String getType() {
		return type;
	}

	public McPanelWsData setType(String type) {
		this.type = type;
		return this;
	}
}
