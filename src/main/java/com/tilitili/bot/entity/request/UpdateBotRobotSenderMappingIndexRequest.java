package com.tilitili.bot.entity.request;

import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class UpdateBotRobotSenderMappingIndexRequest extends BaseDTO {
	private Long senderId;
	private List<Long> botIdList;
	private String indexType;

	public List<Long> getBotIdList() {
		return botIdList;
	}

	public UpdateBotRobotSenderMappingIndexRequest setBotIdList(List<Long> botIdList) {
		this.botIdList = botIdList;
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
