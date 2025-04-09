package com.akira.springbootlogdemo.logging.filter;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ApiLoggingFilter> loggingFilter() {
        FilterRegistrationBean<ApiLoggingFilter> registration = new FilterRegistrationBean<ApiLoggingFilter>();
        registration.setFilter(new ApiLoggingFilter());
        // registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}