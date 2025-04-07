package com.akira.springbootlogdemo.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * for SqlLoggingInterceptorOrg
 */
@Component
@ConfigurationProperties(prefix = "mybatis.logging")
@Data
public class MyBatisLoggingProperties {
    private boolean enabled = true;
    private long slowQueryThreshold = 500;
    private boolean showResults = true;
    private boolean showParams = true;
    private int maxResultLength = 1000;
    private List<String> excludedSqlIds = Collections.emptyList();

    // getters and setters
}