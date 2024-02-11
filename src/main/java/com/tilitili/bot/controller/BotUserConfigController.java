package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotUserConfigUpdateRequest;
import com.tilitili.common.entity.BotUserConfig;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.query.BotUserConfigQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotUserConfigManager;
import com.tilitili.common.mapper.mysql.BotUserConfigMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api/config")
public class BotUserConfigController {
    private final BotUserConfigManager botUserConfigManager;
    private final BotUserConfigMapper botUserConfigMapper;

    public BotUserConfigController(BotUserConfigManager botUserConfigManager, BotUserConfigMapper botUserConfigMapper) {
        this.botUserConfigManager = botUserConfigManager;
        this.botUserConfigMapper = botUserConfigMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<List<BotUserConfig>> listConfig(@SessionAttribute(value = "botUser") BotUserDTO botUser) {
        List<BotUserConfig> configList = botUserConfigMapper.getBotUserConfigByCondition(new BotUserConfigQuery().setUserId(botUser.getId()));
        return BaseModel.success(configList);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateConfig(@SessionAttribute(value = "botUser") BotUserDTO botUser, @RequestBody BotUserConfigUpdateRequest request) {
        List<BotUserConfig> configList = request.getConfigList();
        Asserts.notEmpty(configList, "参数异常");
        for (BotUserConfig config : configList) {
            botUserConfigManager.addOrUpdateConfig(botUser.getId(), config.getKey(), config.getValue());
        }
        return BaseModel.success();
    }
}
