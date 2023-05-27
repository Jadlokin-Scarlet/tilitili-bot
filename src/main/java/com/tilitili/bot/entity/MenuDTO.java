package com.tilitili.bot.entity;

import com.google.common.collect.Lists;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class MenuDTO extends BaseDTO {
    private String label;
    private String key;
    private List<MenuDTO> children;

    public static MenuDTO ofBotMenu(BotMenu botMenu) {
        return new MenuDTO().setKey(botMenu.getPath()).setLabel(botMenu.getName());
    }

    public void addChildren(MenuDTO menuDTO) {
        if (children == null) {
            children = Lists.newArrayList(menuDTO);
        } else {
            children.add(menuDTO);
        }
    }




    public String getLabel() {
        return label;
    }

    public MenuDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getKey() {
        return key;
    }

    public MenuDTO setKey(String key) {
        this.key = key;
        return this;
    }

    public List<MenuDTO> getChildren() {
        return children;
    }

    public MenuDTO setChildren(List<MenuDTO> children) {
        this.children = children;
        return this;
    }
}
