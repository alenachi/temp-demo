package com.akira.springbootlogdemo.logging.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

// https://github.com/LarryDpk/pkslow-samples/blob/master/spring-boot/springboot-common/src/main/java/com/pkslow/springboot/common/web/filter/PkslowBaseFilter.java
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);

    private static final String[] EXCLUDE_PATHS = {"/health"};

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Instant startTime = Instant.now();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        logRequest(wrappedRequest);

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logResponse(wrappedResponse, startTime);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        Map<String, Object> logData = new LinkedHashMap<>();

        // 请求信息
        logData.put("method", request.getMethod());
        logData.put("uri", request.getRequestURI());
        logData.put("query", request.getQueryString());
        logData.put("protocol", request.getScheme());

        // 请求体（非GET请求）
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            var a = request.getParameterMap();
            log.info(request.getParameterMap().toString());
            logData.put("body", getRequestBody(request));
        }

        log.info("[START] Request: {}", logData);
    }

    private void logResponse(ContentCachingResponseWrapper response,Instant startTime) {
        Map<String, Object> logData = new LinkedHashMap<>();
        // 响应信息
        logData.put("status", response.getStatus());
        logData.put("latency", Duration.between(startTime, Instant.now()).toMillis() + "ms");

        // 响应体（仅记录JSON响应）
        if (response.getContentType() != null && response.getContentType().contains("json")) {
            logData.put("responseBody", getResponseBody(response));
        }
        log.info("[End  ] Response: {}", logData);
    }

    // no user
    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }
    // no user
    private Map<String, String> getResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String headerName : response.getHeaderNames()) {
            headers.put(headerName, response.getHeader(headerName));
        }
        return headers;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        return content.length > 0 ? new String(content) : "[empty]";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        return content.length > 0 ? new String(content) : "[empty]";
    }
}