package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotConfigUpdateRequest;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotConfig;
import com.tilitili.common.entity.query.BotConfigQuery;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotConfigManager;
import com.tilitili.common.mapper.mysql.BotConfigMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api/config")
public class BotConfigController {
    private final BotConfigManager botConfigManager;
    private final BotConfigMapper botConfigMapper;

    public BotConfigController(BotConfigManager botConfigManager, BotConfigMapper botConfigMapper) {
        this.botConfigManager = botConfigManager;
        this.botConfigMapper = botConfigMapper;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<List<BotConfig>> listConfig(@SessionAttribute("botAdmin") BotAdmin botAdmin) {
        List<BotConfig> configList = botConfigMapper.getBotConfigByCondition(new BotConfigQuery().setAdminId(botAdmin.getId()));
        return BaseModel.success(configList);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateConfig(@SessionAttribute("botAdmin") BotAdmin botAdmin, @RequestBody BotConfigUpdateRequest request) {
        List<BotConfig> configList = request.getConfigList();
        Asserts.notEmpty(configList, "参数异常");
        for (BotConfig config : configList) {
            botConfigManager.addOrUpdateConfig(botAdmin.getId(), config.getKey(), config.getValue());
        }
        return BaseModel.success();
    }
}
