package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.bot.service.BotService;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.mapper.mysql.BotRobotMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/pub/botReport")
public class BotReportController extends BaseController {
	private final BotRobotMapper botRobotMapper;
	private final BotService botService;

	public BotReportController(BotRobotMapper botRobotMapper, BotService botService) {
		this.botRobotMapper = botRobotMapper;
		this.botService = botService;
	}

	@PostMapping("/{botId}")
	@ResponseBody
	public BaseModel<String> webHooksOther(@RequestBody String requestStr, @PathVariable Long botId) {
		Asserts.notBlank(requestStr, "参数异常");
		Asserts.isTrue(requestStr.endsWith("}"), "参数异常");

		BotRobot bot = botRobotMapper.getValidBotRobotById(botId);
		Asserts.notNull(bot, "啊嘞，不对劲");
		log.info("Message Received {}",requestStr);
		botService.syncHandleMessage(bot, requestStr);
		return BaseModel.success();
	}

}
