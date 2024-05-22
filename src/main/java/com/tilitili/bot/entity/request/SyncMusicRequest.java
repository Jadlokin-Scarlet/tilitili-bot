package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class SyncMusicRequest extends BaseDTO {
	private Long listId;

	public Long getListId() {
		return listId;
	}

	public SyncMusicRequest setListId(Long listId) {
		this.listId = listId;
		return this;
	}
}
