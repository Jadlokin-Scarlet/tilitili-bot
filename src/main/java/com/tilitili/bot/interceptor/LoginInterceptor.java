package com.tilitili.bot.interceptor;

import com.tilitili.bot.controller.BotAdminController;
import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Gsons;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StreamUtil;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@Slf4j
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {
	private final RedisCache redisCache;
	private final BotAdminService botAdminService;

	public LoginInterceptor(RedisCache redisCache, BotAdminService botAdminService) {
		this.redisCache = redisCache;
		this.botAdminService = botAdminService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		HttpSession session = request.getSession();
		BotUserVO botUser = (BotUserVO)session.getAttribute("botUser");

		//登陆和资源下放不用登陆
		String url = request.getRequestURL().toString();
		if (url.contains("/admin")) {
			return true;
		}
		if (url.contains("/pub")) {
			return true;
		}
		// token自动登陆
		String token = request.getCookies() == null? null:
				Arrays.stream(request.getCookies()).filter(StreamUtil.isEqual(Cookie::getName, "token"))
						.map(Cookie::getValue).findFirst().orElse(null);
		if (botUser == null && StringUtils.isNotBlank(token)) {
			Long userId = redisCache.getValueLong(BotAdminController.REMEMBER_TOKEN_KEY + token);
			botUser = botAdminService.getBotUserWithIsAdmin(userId);
			session.setAttribute("botUser", botUser);
		}

		//未登录
		if (botUser == null){
			this.returnResp(response,new BaseModel<>("请重新登录"));
			return false;
		}

		return true;
	}

	private void returnResp(HttpServletResponse response, BaseModel<?> baseModel) {
		PrintWriter writer = null;
		try {
			response.setContentType("application/json;charset=UTF-8");
			writer = response.getWriter();
			writer.print(Gsons.toJson(baseModel));
		} catch (IOException e) {
			log.error("returnResp error, ", e);
		} finally {
			if(writer != null) {
				writer.close();
			}
		}
	}
}
