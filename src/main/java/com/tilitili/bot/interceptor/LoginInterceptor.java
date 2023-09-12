package com.tilitili.bot.interceptor;

import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Gsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        BotAdmin botAdmin = (BotAdmin)session.getAttribute("botAdmin");

        //登陆和资源下放不用登陆
        String url = request.getRequestURL().toString();
        if (url.contains("/admin")) {
            return true;
        }
        if (url.contains("/pub")) {
            return true;
        }

        //未登录
        if (botAdmin == null){
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
