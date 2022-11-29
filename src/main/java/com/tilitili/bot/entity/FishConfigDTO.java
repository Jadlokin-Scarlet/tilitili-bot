package com.tilitili.bot.entity;

import com.tilitili.bot.annotation.ExcelProperty;

public class FishConfigDTO {
	@ExcelProperty(value = "水花规模")
	private String scaleStr;
	@ExcelProperty(value = "奖励类型")
	private String type;
	@ExcelProperty(value = "道具名称")
	private String itemName;
	@ExcelProperty(value = "道具简介")
	private String itemDesc;
	@ExcelProperty(value = "事件文案")
	private String desc;
	@ExcelProperty(value = "价格")
	private Integer price;
	@ExcelProperty(value = "概率")
	private Integer rate;
	@ExcelProperty(value = "成本")
	private Integer cost;
	@ExcelProperty(value = "稀有度")
	private String itemGrade;
	@ExcelProperty(value = "图片")
	private String image;

	public String getScaleStr() {
		return scaleStr;
	}

	public FishConfigDTO setScaleStr(String scaleStr) {
		this.scaleStr = scaleStr;
		return this;
	}

	public String getType() {
		return type;
	}

	public FishConfigDTO setType(String type) {
		this.type = type;
		return this;
	}

	public String getItemName() {
		return itemName;
	}

	public FishConfigDTO setItemName(String itemName) {
		this.itemName = itemName;
		return this;
	}

	public String getItemDesc() {
		return itemDesc;
	}

	public FishConfigDTO setItemDesc(String itemDesc) {
		this.itemDesc = itemDesc;
		return this;
	}

	public String getDesc() {
		return desc;
	}

	public FishConfigDTO setDesc(String desc) {
		this.desc = desc;
		return this;
	}

	public Integer getPrice() {
		return price;
	}

	public FishConfigDTO setPrice(Integer price) {
		this.price = price;
		return this;
	}

	public Integer getRate() {
		return rate;
	}

	public FishConfigDTO setRate(Integer rate) {
		this.rate = rate;
		return this;
	}

	public Integer getCost() {
		return cost;
	}

	public FishConfigDTO setCost(Integer cost) {
		this.cost = cost;
		return this;
	}

	public String getItemGrade() {
		return itemGrade;
	}

	public FishConfigDTO setItemGrade(String itemGrade) {
		this.itemGrade = itemGrade;
		return this;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}
}
