package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotRobotSenderMappingDTO;
import com.tilitili.bot.entity.request.UpdateBotSenderTaskRequest;
import com.tilitili.bot.service.BotSenderService;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRoleAdminMapping;
import com.tilitili.common.entity.query.BotRobotSenderMappingQuery;
import com.tilitili.common.entity.query.BotSenderQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/sender")
public class BotSenderController extends BaseController {
    private final BotSenderService botSenderService;
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotSenderController(BotSenderService botSenderService, BotRoleAdminMappingMapper botRoleAdminMappingMapper) {
        this.botSenderService = botSenderService;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<Map<String, Object>>> listBotSender(@SessionAttribute("botAdmin") BotAdmin botAdmin, BotSenderQuery query) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setAdminId(botAdmin.getId());
        }
        return botSenderService.listBotSender(query);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateBotSenderTask(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody UpdateBotSenderTaskRequest request) {
        botSenderService.updateBotSenderTask(botAdmin, request);
        return BaseModel.success();
    }

    @GetMapping("/botList")
    @ResponseBody
    public BaseModel<PageModel<BotRobotSenderMappingDTO>> getBotSenderBotRobotList(BotRobotSenderMappingQuery query) {
        return botSenderService.getBotSenderBotRobotList(query);
    }
}
