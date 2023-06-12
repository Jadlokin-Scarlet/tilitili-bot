package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.service.BotRobotService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.query.BotRobotQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;

@Controller
@RequestMapping("/api/robot")
public class BotRobotController extends BaseController{
    private final BotRobotService botRobotService;

    public BotRobotController(BotRobotService botRobotService) {
        this.botRobotService = botRobotService;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotRobotDTO>> list(@SessionAttribute("botAdmin") BotAdmin botAdmin, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        return botRobotService.list(botAdmin, query);
    }

    @PostMapping("/up")
    @ResponseBody
    public BaseModel<String> upBot(Long id) {
        botRobotService.upBot(id);
        return BaseModel.success();
    }

    @PostMapping("/down")
    @ResponseBody
    public BaseModel<String> downBot(Long id) {
        botRobotService.downBot(id);
        return BaseModel.success();
    }
}
