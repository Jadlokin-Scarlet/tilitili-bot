package com.tilitili.bot.controller;

import com.tilitili.bot.entity.MenuDTO;
import com.tilitili.bot.entity.request.UpdateRoleMappingRequest;
import com.tilitili.bot.service.BotMenuService;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.BotRoleAdminMapping;
import com.tilitili.common.entity.query.BotMenuQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/menu")
public class BotMenuController extends BaseController {
    private final BotMenuService botMenuService;
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotMenuController(BotMenuService botMenuService, BotRoleAdminMappingMapper botRoleAdminMappingMapper) {
        this.botMenuService = botMenuService;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    @GetMapping("/menuList")
    @ResponseBody
    public BaseModel<List<MenuDTO>> menuList(@SessionAttribute("botAdmin") BotAdmin botAdmin) {
        List<MenuDTO> menuList = botMenuService.getMenuList(botAdmin);
        return BaseModel.success(menuList);
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<Map<String, Object>>> list(@SessionAttribute("botAdmin") BotAdmin botAdmin, BotMenuQuery query) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        Asserts.notNull(adminMapping, "权限不足");
        List<Map<String, Object>> botMenuList = botMenuService.getBotMenuList(query);
        return PageModel.of(botMenuList.size(), botMenuList.size(), 1, botMenuList);
    }

    @PostMapping("/add")
    @ResponseBody
    public BaseModel<?> add(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotMenu botMenu) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        Asserts.notNull(adminMapping, "权限不足");
        botMenuService.addBotMenu(botMenu);
        return BaseModel.success("添加菜单成功");
    }

    @PostMapping("/delete")
    @ResponseBody
    public BaseModel<?> delete(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotMenu botMenu) {
        BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
        Asserts.notNull(adminMapping, "权限不足");
        botMenuService.deleteBotMenu(botMenu);
        return BaseModel.success("删除菜单成功");
    }

    @PostMapping("/role")
    @ResponseBody
    public BaseModel<?> updateMenuMapping(@RequestBody UpdateRoleMappingRequest request) {
        Asserts.notNull(request.getRoleId(), "参数异常");
        Asserts.notNull(request.getMenuId(), "参数异常");
        Asserts.notNull(request.getChecked(), "参数异常");
        botMenuService.updateMenuMapping(request);
        return BaseModel.success();
    }

}
