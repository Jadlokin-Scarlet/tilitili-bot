package com.tilitili.bot.controller;

import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.bot.service.BotSenderService;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/sender")
public class BotSenderController {
    private final BotSenderService botSenderService;

    public BotSenderController(BotSenderService botSenderService) {
        this.botSenderService = botSenderService;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<Map<String, Object>>> listBotSender(BotSenderQuery query) {
        return botSenderService.listBotSender(query);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateBotSenderTask(@RequestBody UpdateBotSenderTaskRequest request) {
        botSenderService.updateBotSenderTask(request);
        return BaseModel.success();
    }
}
