package com.tilitili.bot.service;

import com.google.common.base.Function;
import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.BotAdmin;
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
    private final Map<String, Function<BotAdmin, List<Resource>>> adminResourceMap = new HashMap<>();

    @Autowired
    public ResourceService(BotRoleManager botRoleManager, BotTaskManager botTaskManager, BotSenderService botSenderService) {
        resourceMap.put("robotTypeList", BotRobotConstant::getResource);
        resourceMap.put("sendTypeList", SendTypeEnum::getResource);
        resourceMap.put("roleList", botRoleManager::getResource);
        resourceMap.put("sendTypeResource", SendTypeEnum::getResource);
        resourceMap.put("BotTaskResource", botTaskManager::listTaskResource);
        adminResourceMap.put("botSenderList", botSenderService::listBotSenderResource);
    }

    public List<Resource> getResource(BotAdmin botAdmin, String resourceName) {
        if (adminResourceMap.containsKey(resourceName)) {
            return adminResourceMap.get(resourceName).apply(botAdmin);
        }
        return resourceMap.getOrDefault(resourceName, Collections::emptyList).get();
    }

}
