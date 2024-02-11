package com.tilitili.bot.controller.pub;

import com.tilitili.bot.controller.BaseController;
import com.tilitili.bot.entity.request.ReportCookieRequest;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotUserConfigManager;
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
	private final BotAdminMapper botAdminMapper;
	private final BotUserConfigManager botUserConfigManager;

	public LoginCookieReportController(BotUserConfigManager botUserConfigManager, BotAdminMapper botAdminMapper) {
		this.botAdminMapper = botAdminMapper;
		this.botUserConfigManager = botUserConfigManager;
	}

	@PostMapping("")
	@ResponseBody
	public BaseModel<String> reportCookie(@RequestBody ReportCookieRequest request) {
		Asserts.notNull(request, "参数异常");
		Asserts.notBlank(request.getKey(), "参数异常");
		Asserts.notBlank(request.getCookie(), "参数异常");
		Asserts.notBlank(request.getCode(), "参数异常");
		BotAdmin botAdmin = botAdminMapper.getBotAdminByCode(request.getCode());
		botUserConfigManager.addOrUpdateConfig(botAdmin.getUserId(), request.getKey(), request.getCookie());
		return BaseModel.success();
	}

}
