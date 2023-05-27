package com.tilitili.bot.controller;

import com.tilitili.bot.service.ResourceService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.resource.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;

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
    public BaseModel<?> getResources(@RequestParam List<String> needResourcesList) {
        HashMap<String, List<Resource>> resourceMap = new HashMap<>();
        needResourcesList.forEach(
                resourceName -> resourceMap.put(resourceName, resourceService.getResource(resourceName))
        );
        return BaseModel.success(resourceMap);
    }
}
