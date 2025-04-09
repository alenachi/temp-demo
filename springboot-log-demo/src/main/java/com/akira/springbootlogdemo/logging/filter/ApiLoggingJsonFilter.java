package com.akira.springbootlogdemo.logging.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ApiLoggingJsonFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingJsonFilter.class);
    private static final int MAX_BODY_LENGTH = 2000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    long duration,
                                    Exception ex) {

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("method", request.getMethod());

        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        logMap.put("uri", fullUri);

        String requestBody = getContentAsString(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (!requestBody.isBlank()) {
            logMap.put("requestBody", truncate(requestBody));
        }

        String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());
        logMap.put("status", response.getStatus());
        logMap.put("responseBody", truncate(responseBody));
        logMap.put("durationMs", duration);

        if (ex != null) {
            logMap.put("exception", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        try {
            log.info("API_LOG: {}", objectMapper.writeValueAsString(logMap));
        } catch (Exception e) {
            log.error("Failed to serialize API log", e);
        }
    }

    private String getContentAsString(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        try {
            return new String(buf, encoding != null ? encoding : StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "[UNREADABLE BODY]";
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > MAX_BODY_LENGTH
                ? text.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
                : text;
    }
}
