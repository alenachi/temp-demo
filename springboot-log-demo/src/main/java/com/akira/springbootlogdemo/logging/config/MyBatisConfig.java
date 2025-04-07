package com.akira.springbootlogdemo.logging.config;

import com.akira.springbootlogdemo.logging.mybatis.SqlLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlLoggingInterceptor sqlLoggingInterceptor() {
        return new SqlLoggingInterceptor();
    }
}