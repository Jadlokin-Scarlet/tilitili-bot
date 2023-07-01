package com.tilitili.bot.controller;

import com.tilitili.bot.annotation.BotAuthorityCheck;
import com.tilitili.bot.service.BotForwardService;
import com.tilitili.common.entity.BotForwardConfig;
import com.tilitili.common.entity.query.BotForwardConfigQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/forward")
public class BotForwardController extends BaseController {
    private final BotForwardService botForwardService;

    public BotForwardController(BotForwardService botForwardService) {
        this.botForwardService = botForwardService;
    }

    @GetMapping("/list")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<PageModel<BotForwardConfig>> listForwardConfig(BotForwardConfigQuery query) {
        return botForwardService.listForwardConfig(query);
    }
}
