package com.akira.springbootlogdemo.logging.aop;

import org.apache.coyote.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
     public RequestHandlerinterceptor requestHandlerInterceptor() {
         return new RequestHandlerinterceptor();
     }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestHandlerInterceptor()).addPathPatterns("/**");
    }
}
