package com.tilitili.bot.service;

import com.tilitili.bot.entity.BotMenuDTO;
import com.tilitili.bot.entity.MenuDTO;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.query.BotMenuQuery;
import com.tilitili.common.mapper.mysql.BotMenuMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BotMenuService {
    private final BotMenuMapper botMenuMapper;

    public BotMenuService(BotMenuMapper botMenuMapper) {
        this.botMenuMapper = botMenuMapper;
    }

    public List<MenuDTO> getMenuList() {
        List<BotMenu> menuList = botMenuMapper.getBotMenuByCondition(new BotMenuQuery().setStatus(0));
        menuList.sort(Comparator.comparingInt(BotMenu::getLevel));
        List<MenuDTO> result = new ArrayList<>();
        Map<Long, MenuDTO> idIndexMap = new HashMap<>();
        for (BotMenu botMenu : menuList) {
            MenuDTO menuDTO = MenuDTO.ofBotMenu(botMenu);
            idIndexMap.put(botMenu.getId(), menuDTO);
            if (botMenu.getLevel() == 1) {
                result.add(menuDTO);
            } else if (idIndexMap.containsKey(botMenu.getParentId())) {
                idIndexMap.get(botMenu.getParentId()).addChildren(menuDTO);
            }
        }
        return result;
    }

    public List<BotMenuDTO> getBotMenuList(BotMenuQuery query) {
        List<BotMenu> menuList = botMenuMapper.getBotMenuByCondition(query.setStatus(0));
        menuList.sort(Comparator.comparingInt(BotMenu::getLevel));
        List<BotMenuDTO> result = new ArrayList<>();
        Map<Long, BotMenuDTO> idIndexMap = new HashMap<>();
        for (BotMenu botMenu : menuList) {
            BotMenuDTO botMenuDTO = BotMenuDTO.ofBotMenu(botMenu);
            idIndexMap.put(botMenu.getId(), botMenuDTO);
            if (botMenu.getLevel() == 1) {
                result.add(botMenuDTO);
            } else if (idIndexMap.containsKey(botMenu.getParentId())) {
                idIndexMap.get(botMenu.getParentId()).addChildren(botMenuDTO);
            }
        }
        return result;
    }

    public void addBotMenu(BotMenu botMenu) {
        String path = botMenu.getPath();
        Asserts.notBlank(botMenu.getName(), "菜单名不能为空");
        Asserts.notBlank(path, "菜单路径不能为空");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int pathLevel = path.split("/").length;
        BotMenu otherMenu = botMenuMapper.getBotMenuByPath(path);
        Asserts.checkNull(otherMenu, "该路径已被使用");
        if (botMenu.getParentId() == null) {
            botMenu.setParentId(0L);
            botMenu.setLevel(1);
        } else {
            BotMenu parentMenu = botMenuMapper.getBotMenuById(botMenu.getParentId());
            Asserts.notNull(parentMenu, "父菜单错误");
            botMenu.setLevel(parentMenu.getLevel() + 1);
        }
        Asserts.checkEquals(pathLevel, botMenu.getLevel(), "请正确使用//符号");
        botMenu.setStatus(0);
        botMenuMapper.addBotMenuSelective(botMenu);
    }

    public void deleteBotMenu(BotMenu botMenu) {
        Asserts.notNull(botMenu.getId(), "参数异常");
        botMenuMapper.deleteBotMenuByPrimary(botMenu.getId());
    }
}
