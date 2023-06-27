package com.tilitili.common.manager;

import com.tilitili.common.entity.BotRole;
import com.tilitili.common.entity.query.BotRoleQuery;
import com.tilitili.common.entity.view.resource.Resource;
import com.tilitili.common.mapper.mysql.BotRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BotRoleManager {
    private final BotRoleMapper botRoleMapper;

    public BotRoleManager(BotRoleMapper botRoleMapper) {
        this.botRoleMapper = botRoleMapper;
    }

    public List<Resource> getResource() {
        List<BotRole> roleList = botRoleMapper.getBotRoleByCondition(new BotRoleQuery().setStatus(0));
        return roleList.stream().map(role -> new Resource(role.getId(), role.getName())).collect(Collectors.toList());
    }
}
