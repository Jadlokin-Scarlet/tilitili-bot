package com.tilitili.bot.controller;

import com.tilitili.bot.entity.request.BotAdminRequest;
import com.tilitili.bot.service.BotAdminService;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Asserts;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/api/botAdmin")
public class BotAdminController extends BaseController {
    private final BotAdminService botAdminService;

    public BotAdminController(BotAdminService botAdminService) {
        this.botAdminService = botAdminService;
    }

    @GetMapping("/isLogin")
    @ResponseBody
    public BaseModel<BotAdmin> isLogin(@SessionAttribute(value = "botAdmin", required = false) BotAdmin botAdmin) {
        return new BaseModel<>("", true, botAdmin);
    }

    @PostMapping("/login")
    @ResponseBody
    public BaseModel<?> login(@RequestBody BotAdminRequest request, HttpSession session) {
        Asserts.notNull(request, "参数异常");
        BotAdmin botAdmin = botAdminService.login(request);
        session.setAttribute("botAdmin", botAdmin);
        return new BaseModel<>("登录成功", true, botAdmin);
    }

    @PostMapping("/loginOut")
    @ResponseBody
    public BaseModel<?> loginOut(HttpSession session) {
        session.removeAttribute("botAdmin");
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
