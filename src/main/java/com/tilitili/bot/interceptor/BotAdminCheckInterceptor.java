package com.tilitili.bot.interceptor;

import com.tilitili.bot.annotation.BotAdminCheck;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRoleAdminMapping;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import com.tilitili.common.utils.Asserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@Component
public class BotAdminCheckInterceptor implements HandlerInterceptor {
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotAdminCheckInterceptor(BotRoleAdminMappingMapper botRoleAdminMappingMapper) {
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            BotAdminCheck botAdminCheckAnnotation = handlerMethod.getMethod().getAnnotation(BotAdminCheck.class);

            if (botAdminCheckAnnotation != null) {
                HttpSession session = request.getSession();
                // 从会话中获取数据
                BotAdmin botAdmin = (BotAdmin) session.getAttribute("botAdmin");

                BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
                Asserts.notNull(adminMapping, "权限不足");
            }
        }

        return true;
    }
}