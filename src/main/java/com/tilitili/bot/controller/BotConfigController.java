package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotConfigUpdateRequest;
import com.tilitili.common.entity.BotConfig;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotConfigManager;
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

    public BotConfigController(BotConfigManager botConfigManager) {
        this.botConfigManager = botConfigManager;
    }

    @GetMapping("/list")
    @ResponseBody
    public BaseModel<List<BotConfig>> listConfig(@SessionAttribute(value = "userId") Long userId) {
        List<BotConfig> configList = botConfigManager.listBotConfigByUserIdWithoutPrefix(userId);
        return BaseModel.success(configList);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateConfig(@SessionAttribute(value = "userId") Long userId, @RequestBody BotConfigUpdateRequest request) {
        List<BotConfig> configList = request.getConfigList();
        Asserts.notEmpty(configList, "参数异常");
        for (BotConfig config : configList) {
            botConfigManager.addOrUpdateUserConfig(userId, config.getKey(), config.getValue());
        }
        return BaseModel.success();
    }
}
