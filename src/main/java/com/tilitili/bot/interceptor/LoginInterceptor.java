package com.tilitili.bot.interceptor;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {
	private final RedisCache redisCache;
	private final BotAdminService botAdminService;

	public static final String REMEMBER_TOKEN_KEY = "rememberTokenKey-";
	private static final int TIMEOUT = 60 * 60 * 24 * 30;

	public LoginInterceptor(RedisCache redisCache, BotAdminService botAdminService) {
		this.redisCache = redisCache;
		this.botAdminService = botAdminService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		//登陆和资源下放不用登陆
		String url = request.getRequestURL().toString();
		if (url.contains("/admin")) {
			return true;
		}
		if (url.contains("/pub")) {
			return true;
		}
		// eventSource没有cookie，需要另做认证
		if ("text/event-stream".equals(request.getHeader("Accept"))) {
			return true;
		}
		// 自动登陆
		BotUserVO botUser = this.getSessionUserOrReLoginByToken(request, response);

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

	public BotUserVO getSessionUserOrReLoginByToken(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		BotUserVO botUser = (BotUserVO)session.getAttribute("botUser");
		String token = request.getCookies() == null? null:
				Arrays.stream(request.getCookies()).filter(StreamUtil.isEqual(Cookie::getName, "token"))
						.map(Cookie::getValue).findFirst().orElse(null);
		// 如果登陆状态已失效，但是有token，就自动登陆
		if (botUser == null && StringUtils.isNotBlank(token)) {
			// 消耗token登陆
			botUser = getBotUserUseToken(token);
			session.setAttribute("botUser", botUser);
			// 下发新token
			makeNewToken(response, botUser);
		}
		return botUser;
	}

	// 消耗token，用于自动登陆
	private BotUserVO getBotUserUseToken(String token) {
		Long userId = redisCache.getValueLong(REMEMBER_TOKEN_KEY + token);
		redisCache.delete(REMEMBER_TOKEN_KEY + token);
		Asserts.notNull(userId, "自动登陆失效，请重新登陆");
		return botAdminService.getBotUserWithIsAdmin(userId);
	}

	// 新建token，一式两份，同时创建同时销毁
	public void makeNewToken(HttpServletResponse response, BotUserVO botUser) {
		String newToken = UUID.randomUUID().toString();
		response.addCookie(generateCookie(newToken));
		redisCache.setValue(REMEMBER_TOKEN_KEY+newToken, botUser.getId(), TIMEOUT);
	}

	@NotNull
	public Cookie generateCookie(String newToken) {
		Cookie cookie = new Cookie("token", newToken);
		cookie.setMaxAge(TIMEOUT);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		return cookie;
	}
}
