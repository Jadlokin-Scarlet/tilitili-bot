package com.tilitili.bot.controller;

import com.tilitili.bot.annotation.BotAuthorityCheck;
import com.tilitili.bot.entity.BotRobotDTO;
import com.tilitili.bot.entity.request.BotRobotAddRequest;
import com.tilitili.bot.service.BotRobotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.dto.BotUserDTO;
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
    public BaseModel<PageModel<BotRobotDTO>> list(@SessionAttribute(value = "botUser") BotUserDTO botUser, BotRobotQuery query) throws InvocationTargetException, IllegalAccessException {
        return botRobotService.list(botUser, query);
    }

    @PostMapping("/up/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> upBot(@PathVariable Long botId) {
        botRobotService.upBot(botId);
        return BaseModel.success();
    }

    @PostMapping("/down/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> downBot(@PathVariable Long botId) {
        botRobotService.downBot(botId);
        return BaseModel.success();
    }

    @PostMapping("/add")
    @ResponseBody
    public BaseModel<String> addBot(@SessionAttribute(value = "botUser") BotUserDTO botUser, @RequestBody BotRobot bot) {
        botRobotService.addBot(botUser, bot);
        return BaseModel.success();
    }

    @DeleteMapping("/delete/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> deleteBot(@PathVariable Long botId) {
        botRobotService.deleteBot(botId);
        return BaseModel.success();
    }

    @PostMapping("/edit")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<String> editBot(@RequestBody BotRobotAddRequest bot) {
        botRobotService.editBot(bot);
        return BaseModel.success();
    }

    @GetMapping("/{botId}")
    @ResponseBody
    @BotAuthorityCheck
    public BaseModel<BotRobot> getBot(@PathVariable Long botId) {
        return BaseModel.success(botRobotService.getBot(botId));
    }

//    @PostMapping("/taskList")
//    @ResponseBody
//    @BotAuthorityCheck
//    public BaseModel<BotRobot> updateTaskList(@RequestBody BotRobotAddRequest bot) {
//        botRobotService.updateTaskList(bot);
//        return BaseModel.success();
//    }
}
