package com.tilitili.bot.controller;

import com.tilitili.bot.annotation.BotAdminCheck;
import com.tilitili.bot.entity.MenuDTO;
import com.tilitili.bot.entity.request.UpdateRoleMappingRequest;
import com.tilitili.bot.service.BotMenuService;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotMenuQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/menu")
public class BotMenuController extends BaseController {
    private final BotMenuService botMenuService;

    public BotMenuController(BotMenuService botMenuService) {
        this.botMenuService = botMenuService;
    }

    @GetMapping("/menuList")
    @ResponseBody
    public BaseModel<List<MenuDTO>> menuList(@SessionAttribute(value = "botUser") BotUserDTO botUser) {
        List<MenuDTO> menuList = botMenuService.getMenuList(botUser);
        return BaseModel.success(menuList);
    }

    @GetMapping("/list")
    @ResponseBody
    @BotAdminCheck
    public BaseModel<PageModel<Map<String, Object>>> list(BotMenuQuery query) {
        List<Map<String, Object>> botMenuList = botMenuService.getBotMenuList(query);
        return PageModel.of(botMenuList.size(), botMenuList.size(), 1, botMenuList);
    }

    @PostMapping("/add")
    @ResponseBody
    @BotAdminCheck
    public BaseModel<?> add(@RequestBody BotMenu botMenu) {
        botMenuService.addBotMenu(botMenu);
        return BaseModel.success("添加菜单成功");
    }

    @PostMapping("/delete")
    @ResponseBody
    @BotAdminCheck
    public BaseModel<?> delete(@RequestBody BotMenu botMenu) {
        botMenuService.deleteBotMenu(botMenu);
        return BaseModel.success("删除菜单成功");
    }

    @PostMapping("/role")
    @ResponseBody
    @BotAdminCheck
    public BaseModel<?> updateMenuMapping(@RequestBody UpdateRoleMappingRequest request) {
        Asserts.notNull(request.getRoleId(), "参数异常");
        Asserts.notNull(request.getMenuId(), "参数异常");
        Asserts.notNull(request.getChecked(), "参数异常");
        botMenuService.updateMenuMapping(request);
        return BaseModel.success();
    }

}
