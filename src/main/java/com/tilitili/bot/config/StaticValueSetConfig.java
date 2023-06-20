package com.tilitili.bot.config;

import com.tilitili.common.utils.OSSUtil;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StaticValueSetConfig {
    public StaticValueSetConfig(Environment environment) {
        OSSUtil.setStaticValue(environment);
    }
}
