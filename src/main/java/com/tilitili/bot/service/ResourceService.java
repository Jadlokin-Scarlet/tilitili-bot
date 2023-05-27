package com.tilitili.bot.service;

import com.tilitili.common.entity.view.resource.Resource;
import com.tilitili.common.mapper.rank.RecommendVideoMapper;
import com.tilitili.common.mapper.rank.ResourcesMapper;
import com.tilitili.common.mapper.rank.VideoDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ResourceService {
    private final ResourcesMapper resourcesMapper;
    private final RecommendVideoMapper recommendVideoMapper;
    private final VideoDataMapper videoDataMapper;

    private final Map<String, Supplier<List<Resource>>> resourceMap = new HashMap<>();

    @Autowired
    public ResourceService(ResourcesMapper resourcesMapper, RecommendVideoMapper recommendVideoMapper, VideoDataMapper videoDataMapper) {
        this.resourcesMapper = resourcesMapper;
        this.recommendVideoMapper = recommendVideoMapper;
        this.videoDataMapper = videoDataMapper;
    }

    public List<Resource> getResource(String resourceName) {
        return resourceMap.getOrDefault(resourceName, Collections::emptyList).get();
    }

}
