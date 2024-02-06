package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class ImportRandomTalkRequest extends BaseDTO {
	private String file;

	public String getFile() {
		return file;
	}

	public ImportRandomTalkRequest setFile(String file) {
		this.file = file;
		return this;
	}
}
