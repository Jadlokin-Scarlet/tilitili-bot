package com.tilitili.bot.config;

import com.tilitili.bot.interceptor.LoginInterceptor;
import com.tilitili.bot.interceptor.BotAuthorityCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final BotAuthorityCheckInterceptor botAuthorityCheckInterceptor;

    @Autowired
    public InterceptorConfig(LoginInterceptor loginInterceptor, BotAuthorityCheckInterceptor botAuthorityCheckInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.botAuthorityCheckInterceptor = botAuthorityCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor);
        registry.addInterceptor(botAuthorityCheckInterceptor);
    }

}