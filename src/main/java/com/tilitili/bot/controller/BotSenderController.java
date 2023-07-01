package com.tilitili.bot.controller;

import com.tilitili.bot.annotation.BotAuthorityCheck;
import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.bot.service.BotSenderService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/sender")
public class BotSenderController extends BaseController {
    private final BotSenderService botSenderService;

    public BotSenderController(BotSenderService botSenderService) {
        this.botSenderService = botSenderService;
    }

    @GetMapping("/list")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<PageModel<Map<String, Object>>> listBotSender(BotSenderQuery query) {
        return botSenderService.listBotSender(query);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateBotSenderTask(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody UpdateBotSenderTaskRequest request) {
        botSenderService.updateBotSenderTask(botAdmin, request);
        return BaseModel.success();
    }
}
