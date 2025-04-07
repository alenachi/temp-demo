package com.akira.springbootlogdemo.logging.config;

import com.akira.springbootlogdemo.logging.mybatis.SqlLoggingInterceptorOrg;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MyBatisLoggingProperties.class)
public class MyBatisConig2 {

    @Bean
    public SqlLoggingInterceptorOrg sqlLoggingInterceptor(MyBatisLoggingProperties properties) {
        SqlLoggingInterceptorOrg interceptor = new SqlLoggingInterceptorOrg();

        Properties props = new Properties();
        props.setProperty("slowQueryThreshold", String.valueOf(properties.getSlowQueryThreshold()));
        props.setProperty("showResults", String.valueOf(properties.isShowResults()));
        props.setProperty("showParams", String.valueOf(properties.isShowParams()));
        props.setProperty("maxResultLength", String.valueOf(properties.getMaxResultLength()));

        interceptor.setProperties(props);
        return interceptor;
    }
}
