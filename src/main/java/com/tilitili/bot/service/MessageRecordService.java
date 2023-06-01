package com.tilitili.bot.service;

import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.query.BotMessageRecordQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotMessageRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageRecordService {
    private final BotMessageRecordMapper botMessageRecordMapper;

    public MessageRecordService(BotMessageRecordMapper botMessageRecordMapper) {
        this.botMessageRecordMapper = botMessageRecordMapper;
    }

    public BaseModel<PageModel<BotMessageRecord>> list(BotMessageRecordQuery query) {
        query.setPageSize(20);
        int total = botMessageRecordMapper.countBotMessageRecordByCondition(query);
        List<BotMessageRecord> list = botMessageRecordMapper.getBotMessageRecordByCondition(query);
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), list);
    }
}
