package com.tilitili.bot.interceptor;

import com.tilitili.bot.annotation.BotAdminCheck;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotRoleUserMapping;
import com.tilitili.common.mapper.mysql.BotRoleUserMappingMapper;
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
    private final BotRoleUserMappingMapper botRoleUserMappingMapper;

    public BotAdminCheckInterceptor(BotRoleUserMappingMapper botRoleUserMappingMapper) {
        this.botRoleUserMappingMapper = botRoleUserMappingMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            BotAdminCheck botAdminCheckAnnotation = handlerMethod.getMethod().getAnnotation(BotAdminCheck.class);

            if (botAdminCheckAnnotation != null) {
                HttpSession session = request.getSession();
                // 从会话中获取数据
                Long userId = (Long) session.getAttribute("userId");

                BotRoleUserMapping mapping = botRoleUserMappingMapper.getBotRoleUserMappingByUserIdAndRoleId(userId, BotRoleConstant.adminRole);
                Asserts.notNull(mapping, "权限不足");
            }
        }

        return true;
    }
}