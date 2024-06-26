package com.tilitili.bot.entity;

import com.tilitili.common.entity.dto.BaseDTO;

public class FindImageResult extends BaseDTO {
	private String link;
	private String rate;
	private String imageUrl;

	public String getLink() {
		return link;
	}

	public FindImageResult setLink(String link) {
		this.link = link;
		return this;
	}

	public String getRate() {
		return rate;
	}

	public FindImageResult setRate(String rate) {
		this.rate = rate;
		return this;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public FindImageResult setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		return this;
	}
}
