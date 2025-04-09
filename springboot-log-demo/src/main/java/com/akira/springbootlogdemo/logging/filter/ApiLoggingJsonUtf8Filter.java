package com.akira.springbootlogdemo.logging.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@WebFilter(urlPatterns = "/api/*")
public class ApiLoggingJsonUtf8Filter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingJsonUtf8Filter.class);
    private static final int MAX_BODY_LENGTH = 2000;

    // 使用配置良好的 ObjectMapper
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT); // 格式化输出（人类友好）

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();
        Exception exception = null;

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception ex) {
            exception = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(requestWrapper, responseWrapper, duration, exception);
            responseWrapper.copyBodyToResponse(); // 写回响应
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    long duration,
                                    Exception ex) {

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("请求方法", request.getMethod());

        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        logMap.put("请求地址", fullUri);

        String requestBody = getContentAsString(request.getContentAsByteArray(),request.getCharacterEncoding());
        if (!requestBody.isBlank()) {
            logMap.put("请求体", truncate(requestBody));
        }

        String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());

        logMap.put("响应状态", response.getStatus());
        logMap.put("响应体", truncate(responseBody));
        logMap.put("耗时(ms)", duration);

        if (ex != null) {
            logMap.put("异常信息", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        try {
            // 美化格式 + 保留中文
            String logJson = objectMapper.writeValueAsString(logMap);
            log.info("API日志记录：{}", logJson);
        } catch (JsonProcessingException e) {
            log.error("日志序列化失败", e);
            log.error("日志序列化失败", e);
        }
    }

    private String getContentAsString(byte[] buf, String contentType) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        try {
            // 如果是 JSON 或文本类型，直接用 UTF-8 解码，忽略 response.getCharacterEncoding()
            if (contentType != null && (contentType.contains("application/json") || contentType.contains("text"))) {
                return new String(buf, StandardCharsets.UTF_8);
            } else {
                // 默认用 UTF-8 解码（你可以根据实际情况改）
                return new String(buf, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "[UNREADABLE BODY]";
        }
    }


//    private String getContentAsString(byte[] buf, String contentType, String encoding) {
//        if (buf == null || buf.length == 0) {
//            return "";
//        }
//        try {
//            // 如果 response 没有显式 charset，则尝试从 contentType 中推断 UTF-8
//            if (encoding == null && contentType != null && contentType.toLowerCase().contains("charset=utf-8")) {
//                encoding = StandardCharsets.UTF_8.name();
//            }
//            return new String(buf, encoding != null ? encoding : StandardCharsets.UTF_8.name());
//        } catch (Exception e) {
//            return "[UNREADABLE BODY]";
//        }
//    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > MAX_BODY_LENGTH
                ? text.substring(0, MAX_BODY_LENGTH) + "..."
                : text;
    }
}
