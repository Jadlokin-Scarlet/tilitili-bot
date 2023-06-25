package com.tilitili.bot.entity;

import com.google.common.collect.Lists;
import com.tilitili.common.entity.BotMenu;
import com.tilitili.common.entity.dto.BaseDTO;

import java.util.List;

public class BotMenuDTO extends BaseDTO {
    private Long id;
    private String name;
    private String path;
    private List<BotMenuDTO> children;

    public static BotMenuDTO ofBotMenu(BotMenu botMenu) {
        BotMenuDTO botMenuDTO = new BotMenuDTO();
        botMenuDTO.setId(botMenu.getId());
        botMenuDTO.setName(botMenu.getName());
        botMenuDTO.setPath(botMenu.getPath());
        return botMenuDTO;
    }

    public void addChildren(BotMenuDTO menuDTO) {
        if (children == null) {
            children = Lists.newArrayList(menuDTO);
        } else {
            children.add(menuDTO);
        }
    }

    public List<BotMenuDTO> getChildren() {
        return children;
    }


    public Long getId() {
        return id;
    }

    public BotMenuDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public BotMenuDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getPath() {
        return path;
    }

    public BotMenuDTO setPath(String path) {
        this.path = path;
        return this;
    }
}
