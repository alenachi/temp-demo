package com.akira.springbootlogdemo.logging.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@WebFilter(urlPatterns = "/api/*")
public class LoggingFilter implements Filter {

    private static final int MAX_BODY_LENGTH = 2000;

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
            responseWrapper.copyBodyToResponse(); // 重要：写回原始响应体
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    long duration,
                                    Exception ex) {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        int status = response.getStatus();

        String requestBody = getContentAsString(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());

        System.out.println("======== API LOG START ========");
        System.out.println("[Method] " + method);
        System.out.println("[URI] " + fullUri);

        if (!requestBody.isBlank()) {
            System.out.println("[RequestBody] " + truncate(requestBody));
        }

        System.out.println("[Status] " + status);
        System.out.println("[ResponseBody] " + truncate(responseBody));
        System.out.println("[Duration] " + duration + " ms");

        if (ex != null) {
            System.out.println("[Exception] " + ex.getClass().getName() + ": " + ex.getMessage());
        }

        System.out.println("======== API LOG END ==========");
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
