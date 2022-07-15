package com.tilitili.bot;

import org.apache.ibatis.io.VFS;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableRetry
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = "com.tilitili")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class StartApplication {

    public static void main(String[] args) {
        VFS.addImplClass(SpringBootVFS.class);
        SpringApplication.run(StartApplication.class, args);
    }
}

