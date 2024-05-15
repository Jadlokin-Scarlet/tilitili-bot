package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotFunctionTalkDTO;
import com.tilitili.bot.entity.request.ImportRandomTalkRequest;
import com.tilitili.bot.service.RandomTalkService;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/randomTalk")
public class RandomTalkController extends BaseController {
    public final RandomTalkService randomTalkService;

    public RandomTalkController(RandomTalkService randomTalkService) {
        this.randomTalkService = randomTalkService;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotFunctionTalkDTO>> listRandomTalk(@SessionAttribute(value = "userId") Long userId, BotFunctionTalkQuery query) {
        return randomTalkService.listRandomTalk(query.setAdminUserId(userId));
    }

    @PostMapping("/import")
    @ResponseBody
    public BaseModel<String> importRandomTalk(@SessionAttribute(value = "userId") Long userId, @RequestBody ImportRandomTalkRequest request) {
        Asserts.notBlank(request.getFile(), "参数异常");
        return randomTalkService.importRandomTalk(userId, request.getFile());
    }
}
