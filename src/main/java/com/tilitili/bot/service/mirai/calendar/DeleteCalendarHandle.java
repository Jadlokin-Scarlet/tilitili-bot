package com.tilitili.bot.service.mirai.calendar;

import com.tilitili.bot.entity.bot.BotMessageAction;
import com.tilitili.bot.service.mirai.ExceptionRespMessageHandle;
import com.tilitili.common.entity.BotCalendar;
import com.tilitili.common.entity.view.bot.BotMessage;
import com.tilitili.common.mapper.tilitili.BotCalendarMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteCalendarHandle extends ExceptionRespMessageHandle {
	private final BotCalendarMapper botCalendarMapper;

	@Autowired
	public DeleteCalendarHandle(BotCalendarMapper botCalendarMapper) {
		this.botCalendarMapper = botCalendarMapper;
	}

	@Override
	public BotMessage handleMessage(BotMessageAction messageAction) {
		String cid = messageAction.getValue();
		botCalendarMapper.updateBotCalendarSelective(new BotCalendar().setId(Long.valueOf(cid)).setStatus(-1));
		return BotMessage.simpleTextMessage("移除日程成功。");
	}
}
