package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.bot.entity.request.ReportCookieRequest;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotConfigManager;
import com.tilitili.common.mapper.mysql.BotAdminMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/api/pub/cookie")
public class LoginCookieReportController extends BaseController {
	private final BotConfigManager botConfigManager;
	private final BotAdminMapper botAdminMapper;

	public LoginCookieReportController(BotConfigManager botConfigManager, BotAdminMapper botAdminMapper) {
		this.botConfigManager = botConfigManager;
		this.botAdminMapper = botAdminMapper;
	}

	@PostMapping("")
	@ResponseBody
	public BaseModel<String> reportCookie(@RequestBody ReportCookieRequest request) {
		Asserts.notNull(request, "参数异常");
		Asserts.notBlank(request.getKey(), "参数异常");
		Asserts.notBlank(request.getCookie(), "参数异常");
		Asserts.notBlank(request.getAdminCode(), "参数异常");
		BotAdmin botAdmin = botAdminMapper.getBotAdminByCode(request.getAdminCode());
		botConfigManager.addOrUpdateConfig(botAdmin.getId(), request.getKey(), request.getCookie());
		return BaseModel.success();
	}

}
