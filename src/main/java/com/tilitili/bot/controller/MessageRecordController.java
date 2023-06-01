package com.tilitili.bot.controller;

import com.tilitili.bot.service.MessageRecordService;
import com.tilitili.common.entity.BotMessageRecord;
import com.tilitili.common.entity.query.BotMessageRecordQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/messageRecord")
public class MessageRecordController extends BaseController {

    private final MessageRecordService messageRecordService;

    public MessageRecordController(MessageRecordService messageRecordService) {
        this.messageRecordService = messageRecordService;
    }

    @GetMapping("/list")
    public BaseModel<PageModel<BotMessageRecord>> list(BotMessageRecordQuery query) {
        return messageRecordService.list(query);
    }
}
