package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.RedisCache;
import com.tilitili.common.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
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

    public static final String REMEMBER_TOKEN_KEY = "BotAdminController.rememberTokenKey-";
    private static final int TIMEOUT = 60 * 60 * 24 * 30;

    public BotAdminController(BotAdminService botAdminService, RedisCache redisCache) {
        this.botAdminService = botAdminService;
        this.redisCache = redisCache;
    }

    @GetMapping("/isLogin")
    @ResponseBody
    public BaseModel<BotUserVO> isLogin(@SessionAttribute(value = "botUser", required = false) BotUserVO botUser,
                                         @CookieValue(value = "token", required = false) String token,
                                         HttpSession session) {
        if (botUser == null && StringUtils.isNotBlank(token)) {
            Long userId = redisCache.getValueLong(REMEMBER_TOKEN_KEY + token);
            botUser = botAdminService.getBotUserWithIsAdmin(userId);
            session.setAttribute("botUser", botUser);
        }
        return new BaseModel<>("", true, botUser);
    }

    @PostMapping("/login")
    @ResponseBody
    public BaseModel<BotUserVO> login(@RequestBody BotAdminRequest request, HttpSession session, HttpServletResponse response) {
        Asserts.notNull(request, "参数异常");
        BotUserVO botUser = botAdminService.login(request);
        if (request.getRemember() != null && request.getRemember()) {
            String newToken = UUID.randomUUID().toString();
            response.addCookie(generateCookie(newToken));
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

    @NotNull
    private static Cookie generateCookie(String newToken) {
        Cookie cookie = new Cookie("token", newToken);
        cookie.setMaxAge(TIMEOUT);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        return cookie;
    }

    @PostMapping("/loginOut")
    @ResponseBody
    public BaseModel<?> loginOut(@SessionAttribute(value = "botUser", required = false) BotUserVO botUser, HttpSession session, HttpServletResponse response) {
        session.removeAttribute("botUser");
        response.addCookie(generateCookie(""));
        if (botUser != null) {
            String token = redisCache.getValueString(REMEMBER_TOKEN_KEY + botUser.getId());
            redisCache.delete(REMEMBER_TOKEN_KEY + token);
            redisCache.delete(REMEMBER_TOKEN_KEY + botUser.getId());
        }
        return new BaseModel<>("", true);
    }

    @PostMapping("/checkCode")
    @ResponseBody
    public BaseModel<?> checkCode(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.checkRegisterCode(request.getCode());
        botAdminService.checkMasterQQ(request.getMaster());
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
    public BaseModel<?> register(@RequestBody BotAdminRequest request, HttpSession session, HttpServletResponse response) {
        Asserts.notNull(request, "参数异常");
        botAdminService.register(request);
        return this.login(request, session, response);
    }

    @PostMapping("/checkEmailQuick")
    @ResponseBody
    public BaseModel<?> checkEmailQuick(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.sendEmailCodeQuick(request);
        return BaseModel.success("请在邮箱中查看验证码");
    }

    @PostMapping("/checkEmailCodeQuick")
    @ResponseBody
    public BaseModel<?> checkEmailCodeQuick(@RequestBody BotAdminRequest request) {
        Asserts.notNull(request, "参数异常");
        botAdminService.checkEmailCodeQuick(request);
        return BaseModel.success();
    }

    @PostMapping("/registerQuick")
    @ResponseBody
    public BaseModel<?> registerQuick(@RequestBody BotAdminRequest request, HttpSession session, HttpServletResponse response) {
        Asserts.notNull(request, "参数异常");
        botAdminService.registerQuick(request);
        return this.login(request, session, response);
    }

}
