package com.tilitili.bot.interceptor;

import com.alibaba.fastjson2.JSONObject;
import com.tilitili.bot.annotation.BotAuthorityCheck;
import com.tilitili.common.constant.BotRoleConstant;
import com.tilitili.common.entity.BotAdmin;
import com.tilitili.common.entity.BotRobot;
import com.tilitili.common.entity.BotRoleAdminMapping;
import com.tilitili.common.manager.BotRobotCacheManager;
import com.tilitili.common.mapper.mysql.BotRoleAdminMappingMapper;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@Component
public class BotAuthorityCheckInterceptor implements HandlerInterceptor {

    private final BotRobotCacheManager botRobotCacheManager;
    private final BotRoleAdminMappingMapper botRoleAdminMappingMapper;

    public BotAuthorityCheckInterceptor(BotRobotCacheManager botRobotCacheManager, BotRoleAdminMappingMapper botRoleAdminMappingMapper) {
        this.botRobotCacheManager = botRobotCacheManager;
        this.botRoleAdminMappingMapper = botRoleAdminMappingMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            BotAuthorityCheck botAuthorityCheckAnnotation = handlerMethod.getMethod().getAnnotation(BotAuthorityCheck.class);

            if (botAuthorityCheckAnnotation != null) {
                HttpSession session = request.getSession();
                // 从会话中获取数据
                BotAdmin botAdmin = (BotAdmin) session.getAttribute("botAdmin");
                Asserts.notNull(botAdmin, "参数异常");

                BotRoleAdminMapping adminMapping = botRoleAdminMappingMapper.getBotRoleAdminMappingByAdminIdAndRoleId(botAdmin.getId(), BotRoleConstant.adminRole);
                if (adminMapping == null) {
                    String idStr = this.getBotId(request);
                    Asserts.notNull(idStr, "参数异常");
                    Long id = Long.valueOf(idStr);

                    BotRobot bot = botRobotCacheManager.getBotRobotById(id);
                    Asserts.notNull(bot, "参数异常");
                    Asserts.checkEquals(bot.getAdminId(), botAdmin.getId(), "权限异常");
                }
            }
        }

        return true;
    }

    private String getBotId(HttpServletRequest request) {
        // 获取请求方法和内容类型
        String httpMethod = request.getMethod();

        String value = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE).toString();
        if (StringUtils.isNotBlank(value) && !"{}".equals(value)) {
            return StringUtils.patten1("botId=(\\d+)", value);
        }

        if ("GET".equals(httpMethod)) {
            // 处理GET请求
            String[] requestParamValues = request.getParameterValues("botId");
            if (!ArrayUtils.isEmpty(requestParamValues)) {
                return requestParamValues[0];
            }
            return null;
        } else {
            // 处理POST请求
            String requestBody = ((CustomHttpRequestWrapper)request).getBody();
            return JSONObject.parseObject(requestBody).getString("botId");
        }
    }
}