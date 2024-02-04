package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotFunctionTalkDTO;
import com.tilitili.bot.service.RandomTalkService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/randomTalk")
public class RandomTalkController {
    public final RandomTalkService randomTalkService;

    public RandomTalkController(RandomTalkService randomTalkService) {
        this.randomTalkService = randomTalkService;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotFunctionTalkDTO>> listRandomTalk(@SessionAttribute("botAdmin") BotAdmin botAdmin, BotFunctionTalkQuery query) {
        return randomTalkService.listRandomTalk(query.setAdminId(botAdmin.getId()));
    }

    @PostMapping("/import")
    @ResponseBody
    public BaseModel<String> importRandomTalk(@SessionAttribute("botAdmin") BotAdmin botAdmin, String file) {
        return randomTalkService.importRandomTalk(botAdmin, file);
    }
}
