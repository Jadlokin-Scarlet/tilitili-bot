package com.tilitili.bot.controller;

import com.tilitili.bot.service.ResourceService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.resource.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/resources")
@Validated
@Slf4j
public class ResourceController extends BaseController {
    private final ResourceService resourceService;

    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("")
    @ResponseBody
    public BaseModel<?> getResources(@SessionAttribute(value = "userId") Long userId, @RequestParam List<String> needResourcesList) {
        Map<String, List<Resource>> resourceMap = new HashMap<>();
        for (String resourceName : needResourcesList) {
            resourceMap.put(resourceName, resourceService.getResource(userId, resourceName));
        }
        return BaseModel.success(resourceMap);
    }
}
