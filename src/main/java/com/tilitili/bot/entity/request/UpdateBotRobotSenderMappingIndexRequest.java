package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

public class UpdateBotRobotSenderMappingIndexRequest extends BaseDTO {
	private Long senderId;
	private Long fromBotId;
	private Long toBotId;
	private String indexType;

	public Long getFromBotId() {
		return fromBotId;
	}

	public UpdateBotRobotSenderMappingIndexRequest setFromBotId(Long fromBotId) {
		this.fromBotId = fromBotId;
		return this;
	}

	public Long getToBotId() {
		return toBotId;
	}

	public UpdateBotRobotSenderMappingIndexRequest setToBotId(Long toBotId) {
		this.toBotId = toBotId;
		return this;
	}

	public Long getSenderId() {
		return senderId;
	}

	public UpdateBotRobotSenderMappingIndexRequest setSenderId(Long senderId) {
		this.senderId = senderId;
		return this;
	}

	public String getIndexType() {
		return indexType;
	}

	public UpdateBotRobotSenderMappingIndexRequest setIndexType(String indexType) {
		this.indexType = indexType;
		return this;
	}
}
