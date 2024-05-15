package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotRobotSenderMappingDTO;
import com.tilitili.bot.entity.request.UpdateBotRobotSenderMappingIndexRequest;
import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.bot.service.BotSenderService;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotRoleUserMapping;
import com.tilitili.common.entity.query.BotRobotSenderMappingQuery;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRoleUserMappingMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/sender")
public class BotSenderController extends BaseController {
    private final BotSenderService botSenderService;
    private final BotRoleUserMappingMapper botRoleUserMappingMapper;

    public BotSenderController(BotSenderService botSenderService, BotRoleUserMappingMapper botRoleUserMappingMapper) {
        this.botSenderService = botSenderService;
        this.botRoleUserMappingMapper = botRoleUserMappingMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<Map<String, Object>>> listBotSender(@SessionAttribute(value = "userId") Long userId, BotSenderQuery query) {
        BotRoleUserMapping adminMapping = botRoleUserMappingMapper.getBotRoleUserMappingByUserIdAndRoleId(userId, BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setAdminUserId(userId);
        }
        return botSenderService.listBotSender(query);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateBotSenderTask(@SessionAttribute(value = "userId") Long userId, @RequestBody UpdateBotSenderTaskRequest request) {
        botSenderService.updateBotSenderTask(userId, request);
        return BaseModel.success();
    }

    @GetMapping("/bot/list")
    @ResponseBody
    public BaseModel<PageModel<BotRobotSenderMappingDTO>> getBotSenderBotRobotList(BotRobotSenderMappingQuery query) {
        return botSenderService.getBotSenderBotRobotList(query);
    }

    @PostMapping("/bot/update")
    @ResponseBody
    public BaseModel<?> updateBotSenderBotRobotIndex(@RequestBody UpdateBotRobotSenderMappingIndexRequest request) {
        botSenderService.updateBotSenderBotRobotIndex(request);
        return BaseModel.success();
    }
}
