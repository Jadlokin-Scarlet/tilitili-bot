package com.tilitili.bot.entity;

import java.util.List;
import java.util.Map;

public class ExcelResult<T> {
	private String title;
	private List<T> resultList;
	private List<List<String>> data;
	private Map<String, String> paramMap;

	public List<T> getResultList() {
		return resultList;
	}

	public ExcelResult setResultList(List<T> resultList) {
		this.resultList = resultList;
		return this;
	}

	public Map<String, String> getParamMap() {
		return paramMap;
	}

	public ExcelResult setParamMap(Map<String, String> paramMap) {
		this.paramMap = paramMap;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public ExcelResult setTitle(String title) {
		this.title = title;
		return this;
	}

	public List<List<String>> getData() {
		return data;
	}

	public ExcelResult setData(List<List<String>> data) {
		this.data = data;
		return this;
	}
}
