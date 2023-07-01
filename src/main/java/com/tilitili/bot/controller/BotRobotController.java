package com.tilitili.bot.controller;

import com.tilitili.bot.annotation.BotAuthorityCheck;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.entity.request.BotRobotAddRequest;
import com.tilitili.bot.service.BotRobotService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRobot;
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

    @PostMapping("/up/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> upBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @PathVariable Long botId) {
        botRobotService.upBot(botAdmin, botId);
        return BaseModel.success();
    }

    @PostMapping("/down/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> downBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @PathVariable Long botId) {
        botRobotService.downBot(botAdmin, botId);
        return BaseModel.success();
    }

    @PostMapping("/add")
    @ResponseBody
    public BaseModel<String> addBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotRobot bot) {
        botRobotService.addBot(botAdmin, bot);
        return BaseModel.success();
    }

    @DeleteMapping("/delete/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> deleteBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @PathVariable Long botId) {
        botRobotService.deleteBot(botAdmin, botId);
        return BaseModel.success();
    }

    @PostMapping("/edit")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> editBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotRobotAddRequest bot) {
        bot.setId(bot.getBotId());
        botRobotService.editBot(botAdmin, bot);
        return BaseModel.success();
    }

    @GetMapping("/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<BotRobot> getBot(@SessionAttribute("botAdmin") BotAdmin botAdmin, @PathVariable Long botId) {
        return BaseModel.success(botRobotService.getBot(botAdmin, botId));
    }
}
