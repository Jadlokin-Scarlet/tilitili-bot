package com.tilitili.bot.controller;

import com.tilitili.bot.service.BotUserService;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRoleAdminMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/user")
public class BotUserController extends BaseController {
    private final BotUserService botUserService;
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotUserController(BotUserService botUserService, BotRoleAdminMappingMapper botRoleAdminMappingMapper) {
        this.botUserService = botUserService;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotUserDTO>> listBotUser(@SessionAttribute("botAdmin") BotAdmin botAdmin, BotUserQuery query) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setAdminId(botAdmin.getId());
        }
        return botUserService.listBotUser(query);
    }

    @PostMapping("/status")
    @ResponseBody
    public BaseModel<?> changeStatus(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotUserDTO updBotUser) {
        botUserService.changeStatus(botAdmin.getId(), updBotUser);
        return BaseModel.success();
    }

    @PostMapping("/bind")
    @ResponseBody
    public BaseModel<?> bindUser(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotUserDTO updBotUser) {
        botUserService.bindUser(botAdmin.getId(), updBotUser.getId(), updBotUser.getQq());
        return BaseModel.success();
    }
}
