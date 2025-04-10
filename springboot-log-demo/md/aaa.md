package com.example.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@WebFilter(urlPatterns = "/*")
public class ApiLoggingFilter implements Filter {

    private static final int MAX_LOG_LENGTH = 5000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpServletRequest) ||
            !(response instanceof HttpServletResponse httpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpServletResponse);

        long startTime = Instant.now().toEpochMilli();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = Instant.now().toEpochMilli() - startTime;
            Map<String, Object> logMap = new HashMap<>();

            // 请求基本信息
            logMap.put("method", wrappedRequest.getMethod());
            logMap.put("uri", wrappedRequest.getRequestURI());
            logMap.put("params", wrappedRequest.getParameterMap());
            logMap.put("status", wrappedResponse.getStatus());
            logMap.put("duration_ms", duration);

            // 请求体
            String reqContentType = wrappedRequest.getContentType();
            if (!isMultipart(reqContentType)) {
                String requestBody = getContent(wrappedRequest.getContentAsByteArray(), reqContentType);
                logMap.put("request_body", truncate(requestBody));
            } else {
                logMap.put("request_body", "[multipart/form-data omitted]");
            }

            // 响应体
            String respContentType = wrappedResponse.getContentType();
            if (!isBinaryContent(respContentType)) {
                String responseBody = getContent(wrappedResponse.getContentAsByteArray(), respContentType);
                logMap.put("response_body", truncate(responseBody));
            } else {
                logMap.put("response_body", "[binary response omitted]");
            }

            // 日志输出（JSON 美化格式）
            System.out.println("API Access Log:\n" +
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logMap));

            // 写回响应体
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getContent(byte[] content, String contentType) {
        if (content == null || content.length == 0) return "";
        try {
            return new String(content, getCharset(contentType));
        } catch (Exception e) {
            return "[unreadable content]";
        }
    }

    private java.nio.charset.Charset getCharset(String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("charset=")) {
            try {
                String charset = contentType.substring(contentType.toLowerCase().indexOf("charset=") + 8).trim();
                return java.nio.charset.Charset.forName(charset);
            } catch (Exception ignored) {}
        }
        return StandardCharsets.UTF_8;
    }

    private boolean isMultipart(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private boolean isBinaryContent(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("application/octet-stream") ||
               ct.contains("application/pdf") ||
               ct.contains("application/zip") ||
               ct.contains("image/") ||
               ct.contains("video/") ||
               ct.contains("audio/") ||
               ct.contains("excel");
    }

    private String truncate(String text) {
        if (text.length() > MAX_LOG_LENGTH) {
            return text.substring(0, MAX_LOG_LENGTH) + "...[truncated]";
        }
        return text;
    }
}