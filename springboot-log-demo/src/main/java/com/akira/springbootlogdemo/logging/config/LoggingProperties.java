package com.akira.springbootlogdemo.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * for ControllerLoggingAspectOrg
 */
@Component
@ConfigurationProperties(prefix = "logging.controller")
@Data
public class LoggingProperties {
    private boolean enabled = true;
    private boolean includeErrorStacktrace = false;
    private List<String> excludedHeaders = Arrays.asList("authorization", "cookie");
    private List<String> excludedParameterTypes = Collections.singletonList("org.springframework.ui.Model");

    // getters and setters
}