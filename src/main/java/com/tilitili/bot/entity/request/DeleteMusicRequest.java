package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class DeleteMusicRequest extends BaseDTO {
	private Long listId;

	public Long getListId() {
		return listId;
	}

	public DeleteMusicRequest setListId(Long listId) {
		this.listId = listId;
		return this;
	}
}
