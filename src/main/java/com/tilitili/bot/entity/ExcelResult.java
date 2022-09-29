package com.tilitili.bot.entity;

import java.util.List;
import java.util.Map;

public class ExcelResult<T> {
	private String title;
	private List<T> resultList;
	private List<List<List<String>>> data;
	private Map<String, String> paramMap;

	public String getParam(Object key) {
		return paramMap.get(key);
	}

	public List<T> getResultList() {
		return resultList;
	}

	public ExcelResult<T> setResultList(List<T> resultList) {
		this.resultList = resultList;
		return this;
	}

	public Map<String, String> getParamMap() {
		return paramMap;
	}

	public ExcelResult<T> setParamMap(Map<String, String> paramMap) {
		this.paramMap = paramMap;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public ExcelResult<T> setTitle(String title) {
		this.title = title;
		return this;
	}

	public List<List<List<String>>> getData() {
		return data;
	}

	public ExcelResult<T> setData(List<List<List<String>>> data) {
		this.data = data;
		return this;
	}
}
