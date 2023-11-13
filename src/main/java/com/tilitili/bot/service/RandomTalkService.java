package com.tilitili.bot.service;

import com.tilitili.bot.entity.BotFunctionTalkDTO;
import com.tilitili.common.entity.BotFunctionTalk;
import com.tilitili.common.entity.BotSender;
import com.tilitili.common.entity.query.BotFunctionTalkQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.entity.view.PageModel;
import com.tilitili.common.mapper.mysql.BotFunctionTalkMapper;
import com.tilitili.common.manager.BotSenderCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RandomTalkService {
    private final BotFunctionTalkMapper botFunctionTalkMapper;
    private final BotSenderCacheManager botSenderCacheManager;

    public RandomTalkService(BotFunctionTalkMapper botFunctionTalkMapper, BotSenderCacheManager botSenderCacheManager) {
        this.botFunctionTalkMapper = botFunctionTalkMapper;
        this.botSenderCacheManager = botSenderCacheManager;
    }

    public BaseModel<PageModel<BotFunctionTalkDTO>> listRandomTalk(BotFunctionTalkQuery query) {
        query.setStatus(0);
        List<BotFunctionTalk> list = botFunctionTalkMapper.getBotFunctionTalkByAdmin(query);
        int total = botFunctionTalkMapper.countBotFunctionTalkByAdmin(query);
        Map<Long, BotSender> cacheMap = new HashMap<>();
        List<BotFunctionTalkDTO> result = list.stream().map(functionTalk -> {
            BotFunctionTalkDTO functionTalkDTO = new BotFunctionTalkDTO();
            BotSender botSender = cacheMap.computeIfAbsent(functionTalk.getSenderId(), botSenderCacheManager::getValidBotSenderById);
            if (botSender == null) {
                log.warn("{}渠道不存在", functionTalk.getSenderId());
                return null;
            }
            functionTalkDTO.setId(functionTalk.getId());
            functionTalkDTO.setSenderName(botSender.getName());
            functionTalkDTO.setSendType(botSender.getSendType());
            functionTalkDTO.setReq(functionTalk.getReq());
            functionTalkDTO.setResp(functionTalk.getResp());
            functionTalkDTO.setFunction(functionTalk.getFunction());
            return functionTalkDTO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return PageModel.of(total, query.getPageSize(), query.getCurrent(), result);
    }
}
