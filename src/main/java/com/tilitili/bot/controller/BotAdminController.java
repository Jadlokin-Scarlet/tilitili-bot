package com.tilitili.bot.controller;

import com.tilitili.bot.entity.BotUserVO;
import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.bot.interceptor.LoginInterceptor;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/api/admin")
public class BotAdminController extends BaseController {
    private final BotAdminService botAdminService;
    private final LoginInterceptor loginInterceptor;

    public BotAdminController(BotAdminService botAdminService, LoginInterceptor loginInterceptor) {
        this.botAdminService = botAdminService;
        this.loginInterceptor = loginInterceptor;
    }

    @GetMapping("/isLogin")
    @ResponseBody
    public BaseModel<BotUserVO> isLogin(HttpServletRequest request, HttpServletResponse response) {
        Long userId = loginInterceptor.getSessionUserOrReLoginByToken(request, response);
        BotUserVO botUser = botAdminService.getBotUserWithIsAdmin(userId);
        return new BaseModel<>("", true, botUser);
    }

    @PostMapping("/login")
    @ResponseBody
    public BaseModel<BotUserVO> login(@RequestBody BotAdminRequest request, HttpSession session, HttpServletResponse response) {
        Asserts.notNull(request, "参数异常");
        BotUserVO botUser = botAdminService.login(request);
        // 直接下发新token，不管有没有旧token
        if (request.getRemember() != null && request.getRemember()) {
            loginInterceptor.makeNewToken(response, botUser.getId());
        }
        session.setAttribute("userId", botUser.getId());
        return new BaseModel<>("登录成功", true, botUser);
    }

    @PostMapping("/loginOut")
    @ResponseBody
    public BaseModel<?> loginOut(HttpSession session, HttpServletResponse response) {
        session.removeAttribute("userId");
        response.addCookie(loginInterceptor.generateCookie(""));
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
