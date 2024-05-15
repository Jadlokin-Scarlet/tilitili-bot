package com.tilitili.bot.controller;

import com.tilitili.bot.service.BotUserService;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotRoleUserMapping;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRoleUserMappingMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/user")
public class BotUserController extends BaseController {
    private final BotUserService botUserService;
    private final BotRoleUserMappingMapper botRoleUserMappingMapper;

    public BotUserController(BotUserService botUserService, BotRoleUserMappingMapper botRoleUserMappingMapper) {
        this.botUserService = botUserService;
        this.botRoleUserMappingMapper = botRoleUserMappingMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotUserDTO>> listBotUser(@SessionAttribute(value = "userId") Long userId, BotUserQuery query) {
        BotRoleUserMapping adminMapping = botRoleUserMappingMapper.getBotRoleUserMappingByUserIdAndRoleId(userId, BotRoleConstant.adminRole);
        if (adminMapping == null) {
            query.setAdminUserId(userId);
        }
        return botUserService.listBotUser(query);
    }

    @PostMapping("/status")
    @ResponseBody
    public BaseModel<?> changeStatus(@SessionAttribute(value = "userId") Long userId, @RequestBody BotUserDTO updBotUser) {
        botUserService.changeStatus(userId, updBotUser);
        return BaseModel.success();
    }

    @PostMapping("/bind")
    @ResponseBody
    public BaseModel<?> bindUser(@SessionAttribute(value = "userId") Long userId, @RequestBody BotUserDTO updBotUser) {
        botUserService.bindUser(userId, updBotUser.getId(), updBotUser.getQq());
        return BaseModel.success();
    }
}
