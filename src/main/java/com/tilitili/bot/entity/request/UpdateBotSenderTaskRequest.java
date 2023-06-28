package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class UpdateBotSenderTaskRequest extends BaseDTO {
	private Long id;
	private Long taskId;
	private Boolean checked;

	public Long getId() {
		return id;
	}

	public UpdateBotSenderTaskRequest setId(Long id) {
		this.id = id;
		return this;
	}

	public Long getTaskId() {
		return taskId;
	}

	public UpdateBotSenderTaskRequest setTaskId(Long taskId) {
		this.taskId = taskId;
		return this;
	}

	public Boolean getChecked() {
		return checked;
	}

	public UpdateBotSenderTaskRequest setChecked(Boolean checked) {
		this.checked = checked;
		return this;
	}
}
