package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotMenuDTO;
import com.tilitili.bot.entity.MenuDTO;
import com.tilitili.bot.service.BotMenuService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/botMenu")
public class BotMenuController extends BaseController {
    private final BotMenuService botMenuService;

    public BotMenuController(BotMenuService botMenuService) {
        this.botMenuService = botMenuService;
    }

    @GetMapping("/menuList")
    @ResponseBody
    public BaseModel<List<MenuDTO>> menuList(@SessionAttribute("botAdmin") BotAdmin botAdmin) {
        List<MenuDTO> menuList = botMenuService.getMenuList();
        return BaseModel.success(menuList);
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<PageModel<BotMenuDTO>> list() {
        List<BotMenuDTO> botMenuList = botMenuService.getBotMenuList();
        return PageModel.of(botMenuList.size(), botMenuList.size(), 1, botMenuList);
    }

    @PostMapping("/add")
    public BaseModel<?> add(@RequestBody BotMenu botMenu) {
        botMenuService.addBotMenu(botMenu);
        return BaseModel.success("添加菜单成功");
    }


}
