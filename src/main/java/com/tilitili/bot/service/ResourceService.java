package com.tilitili.bot.service;

import com.tilitili.common.constant.BotRobotConstant;
import com.tilitili.common.emnus.SendTypeEnum;
import com.tilitili.common.entity.view.resource.Resource;
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

    @Autowired
    public ResourceService() {
        resourceMap.put("robotTypeList", BotRobotConstant::getResource);
        resourceMap.put("sendTypeList", SendTypeEnum::getResource);
    }

    public List<Resource> getResource(String resourceName) {
        return resourceMap.getOrDefault(resourceName, Collections::emptyList).get();
    }

}
