package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotConfigUpdateRequest;
import com.tilitili.common.entity.BotConfig;
import com.tilitili.common.entity.dto.BotUserDTO;
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
    public BaseModel<List<BotConfig>> listConfig(@SessionAttribute(value = "botUser") BotUserDTO botUser) {
        List<BotConfig> configList = botConfigManager.listBotConfigByUserIdWithoutPrefix(botUser.getId());
        return BaseModel.success(configList);
    }

    @PostMapping("/update")
    @ResponseBody
    public BaseModel<?> updateConfig(@SessionAttribute(value = "botUser") BotUserDTO botUser, @RequestBody BotConfigUpdateRequest request) {
        List<BotConfig> configList = request.getConfigList();
        Asserts.notEmpty(configList, "参数异常");
        for (BotConfig config : configList) {
            botConfigManager.addOrUpdateUserConfig(botUser.getId(), config.getKey(), config.getValue());
        }
        return BaseModel.success();
    }
}
