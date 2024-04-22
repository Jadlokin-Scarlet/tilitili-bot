package com.tilitili.bot.controller;

import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.dto.BotUserDTO;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.manager.BotUserManager;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
@RequestMapping("/api/admin")
public class BotAdminController extends BaseController {
    private final BotAdminService botAdminService;
    private final RedisCache redisCache;
    private final BotUserManager botUserManager;

    private static final String REMEMBER_TOKEN_KEY = "BotAdminController.rememberTokenKey-";
    private static final int TIMEOUT = 60 * 60 * 24 * 30;

    public BotAdminController(BotAdminService botAdminService, RedisCache redisCache, BotUserManager botUserManager) {
        this.botAdminService = botAdminService;
        this.redisCache = redisCache;
        this.botUserManager = botUserManager;
    }

    @GetMapping("/isLogin")
    @ResponseBody
    public BaseModel<BotUserDTO> isLogin(@SessionAttribute(value = "botUser", required = false) BotUserDTO botUser,
                                         @CookieValue(value = "token", required = false) String token,
                                         HttpSession session) {
        if (botUser == null && token != null) {
            Long userId = redisCache.getValueLong(REMEMBER_TOKEN_KEY + token);
            botUser = botUserManager.getValidBotUserByIdWithParent(userId);
            session.setAttribute("botUser", botUser);
        }
        return new BaseModel<>("", true, botUser);
    }

    @PostMapping("/login")
    @ResponseBody
    public BaseModel<BotUserDTO> login(@RequestBody BotAdminRequest request, HttpSession session, HttpServletResponse response) {
        Asserts.notNull(request, "参数异常");
        BotUserDTO botUser = botAdminService.login(request);
        if (request.getRemember() != null && request.getRemember()) {
            String newToken = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("token", newToken);
            cookie.setMaxAge(TIMEOUT);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            redisCache.setValue(REMEMBER_TOKEN_KEY+newToken, botUser.getId(), TIMEOUT);
            redisCache.setValue(REMEMBER_TOKEN_KEY+botUser.getId(), newToken, TIMEOUT);
        } else {
            String token = redisCache.getValueString(REMEMBER_TOKEN_KEY + botUser.getId());
            redisCache.delete(REMEMBER_TOKEN_KEY+token);
            redisCache.delete(REMEMBER_TOKEN_KEY+botUser.getId());
            response.addCookie(new Cookie("token", ""));
        }
        session.setAttribute("botUser", botUser);
        return new BaseModel<>("登录成功", true, botUser);
    }

    @PostMapping("/loginOut")
    @ResponseBody
    public BaseModel<?> loginOut(@SessionAttribute(value = "botUser", required = false) BotUserDTO botUser, HttpSession session, HttpServletResponse response) {
        session.removeAttribute("botUser");
        String token = redisCache.getValueString(REMEMBER_TOKEN_KEY + botUser.getId());
        redisCache.delete(REMEMBER_TOKEN_KEY+token);
        redisCache.delete(REMEMBER_TOKEN_KEY+botUser.getId());
        response.addCookie(new Cookie("token", ""));
        return new BaseModel<>("", true);
    }

    @PostMapping("/checkCode")
    @ResponseBody
    public BaseModel<?> checkCode(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.checkRegisterCode(request.getCode());
        return BaseModel.success("");
    }

    @PostMapping("/checkEmail")
    @ResponseBody
    public BaseModel<?> checkEmail(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.sendEmailCode(request);
        return BaseModel.success("请在邮箱中查看验证码");
    }

    @PostMapping("/checkEmailCode")
    @ResponseBody
    public BaseModel<?> checkEmailCode(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.checkEmailCode(request);
        return BaseModel.success();
    }

    @PostMapping("/register")
    @ResponseBody
    public BaseModel<?> register(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.register(request);
        return BaseModel.success("注册成功");
    }

}
