package com.tilitili.bot.service;

import com.google.common.base.Function;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.constant.BotUserConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.resource.Resource;
import com.tilitili.common.manager.BotRoleManager;
import com.tilitili.common.manager.BotTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ResourceService {
    private final Map<String, Supplier<List<Resource>>> resourceMap = new HashMap<>();
    private final Map<String, Function<BotUserDTO, List<Resource>>> adminResourceMap = new HashMap<>();

    @Autowired
    public ResourceService(BotRoleManager botRoleManager, BotTaskManager botTaskManager, BotSenderService botSenderService) {
        resourceMap.put("robotTypeList", BotRobotConstant::getResource);
        resourceMap.put("sendTypeList", SendTypeEnum::getResource);
        resourceMap.put("roleList", botRoleManager::getResource);
        resourceMap.put("sendTypeResource", SendTypeEnum::getResource);
        resourceMap.put("botTaskResource", botTaskManager::listTaskResource);
        resourceMap.put("userTypeList", BotUserConstant::getResource);
        adminResourceMap.put("botSenderList", botSenderService::listBotSenderResource);
    }

    public List<Resource> getResource(BotUserDTO botUser, String resourceName) {
        if (adminResourceMap.containsKey(resourceName)) {
            return adminResourceMap.get(resourceName).apply(botUser);
        }
        return resourceMap.getOrDefault(resourceName, Collections::emptyList).get();
    }

}
