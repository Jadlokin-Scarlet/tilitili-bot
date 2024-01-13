package com.tilitili.bot.entity.bot.table;

import com.tilitili.common.entity.dto.BaseDTO;

public class Card extends BaseDTO {
	private String type;
	private String title;
	private String img;

	public String getType() {
		return type;
	}

	public Card setType(String type) {
		this.type = type;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public Card setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getImg() {
		return img;
	}

	public Card setImg(String img) {
		this.img = img;
		return this;
	}
}
