package com.tilitili.bot.service;

import com.google.common.collect.Lists;
import com.tilitili.bot.entity.MenuDTO;
import com.tilitili.bot.entity.request.UpdateRoleMappingRequest;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.BotMenuRoleMapping;
import com.tilitili.common.entity.BotRole;
import com.tilitili.common.entity.query.BotMenuQuery;
import com.tilitili.common.entity.query.BotMenuRoleMappingQuery;
import com.tilitili.common.mapper.mysql.BotMenuMapper;
import com.tilitili.common.mapper.mysql.BotMenuRoleMappingMapper;
import com.tilitili.common.mapper.mysql.BotRoleMapper;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BotMenuService {
    private final BotMenuMapper botMenuMapper;
    private final BotMenuRoleMappingMapper botMenuRoleMappingMapper;
    private final BotRoleMapper botRoleMapper;

    public BotMenuService(BotMenuMapper botMenuMapper, BotMenuRoleMappingMapper botMenuRoleMappingMapper, BotRoleMapper botRoleMapper) {
        this.botMenuMapper = botMenuMapper;
        this.botMenuRoleMappingMapper = botMenuRoleMappingMapper;
        this.botRoleMapper = botRoleMapper;
    }

    public List<MenuDTO> getMenuList(Long userId) {
        List<BotMenu> menuList = botMenuMapper.getUserMenuList(userId);
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

    public List<Map<String, Object>> getBotMenuList(BotMenuQuery query) {
        Map<Long, BotRole> roleCacheMap = new HashMap<>();
        List<BotMenu> menuList = botMenuMapper.getBotMenuByCondition(query.setStatus(0));
        List<BotMenuRoleMapping> menuMappingAllList = botMenuRoleMappingMapper.getBotMenuRoleMappingByCondition(new BotMenuRoleMappingQuery());
        Map<Long, List<BotMenuRoleMapping>> menuMappingMap = menuMappingAllList.stream().collect(Collectors.groupingBy(BotMenuRoleMapping::getMenuId));
        menuList.sort(Comparator.comparingInt(BotMenu::getLevel));

        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, Map<String, Object>> idIndexMap = new HashMap<>();
        for (BotMenu botMenu : menuList) {
            Map<String, Object> botMenuDTO = new HashMap<>();
            botMenuDTO.put("id", botMenu.getId());
            botMenuDTO.put("name", botMenu.getName());
            botMenuDTO.put("path", botMenu.getPath());
            idIndexMap.put(botMenu.getId(), botMenuDTO);

            List<BotMenuRoleMapping> menuMappingList = menuMappingMap.getOrDefault(botMenu.getId(), Collections.emptyList());
            for (BotMenuRoleMapping menuMapping : menuMappingList) {
                BotRole role = roleCacheMap.computeIfAbsent(menuMapping.getRoleId(), botRoleMapper::getBotRoleById);
                botMenuDTO.put(String.valueOf(role.getId()), Boolean.TRUE);
            }

            if (botMenu.getLevel() == 1) {
                result.add(botMenuDTO);
            } else if (idIndexMap.containsKey(botMenu.getParentId())) {
                ((List<Map<String, Object>>)idIndexMap.get(botMenu.getParentId()).computeIfAbsent("children", key -> Lists.newArrayList())).add(botMenuDTO);
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
        BotMenu parentMenu = null;
        if (botMenu.getParentId() == null) {
            botMenu.setParentId(0L);
            botMenu.setLevel(1);
        } else {
            parentMenu = botMenuMapper.getBotMenuById(botMenu.getParentId());
            Asserts.notNull(parentMenu, "父菜单错误");
            botMenu.setLevel(parentMenu.getLevel() + 1);
        }
        Asserts.checkEquals(pathLevel, botMenu.getLevel(), "请正确使用//符号");
        botMenu.setStatus(0);
        botMenuMapper.addBotMenuSelective(botMenu);
        if (parentMenu != null) {
            List<BotMenuRoleMapping> mappingList = botMenuRoleMappingMapper.getBotMenuRoleMappingByCondition(new BotMenuRoleMappingQuery().setMenuId(parentMenu.getId()));
            List<Long> roleIdList = mappingList.stream().map(BotMenuRoleMapping::getRoleId).collect(Collectors.toList());
            for (Long roleId : roleIdList) {
                botMenuRoleMappingMapper.addBotMenuRoleMappingSelective(new BotMenuRoleMapping().setMenuId(botMenu.getId()).setRoleId(roleId));
            }
        }
    }

    public void deleteBotMenu(BotMenu botMenu) {
        Asserts.notNull(botMenu.getId(), "参数异常");
        botMenuMapper.deleteBotMenuByPrimary(botMenu.getId());
    }

    public void updateMenuMapping(UpdateRoleMappingRequest request) {
        BotMenuRoleMapping dbMapping = botMenuRoleMappingMapper.getBotMenuRoleMappingByRoleIdAndMenuId(request.getRoleId(), request.getMenuId());
        if (request.getChecked() && dbMapping == null) {
            botMenuRoleMappingMapper.addBotMenuRoleMappingSelective(request);
        } else if(!request.getChecked() && dbMapping != null) {
            botMenuRoleMappingMapper.deleteBotMenuRoleMappingByPrimary(dbMapping.getId());
        }
    }
}
