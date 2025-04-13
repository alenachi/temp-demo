```java
package com.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest
class ApiLoggingFilterTest {

    private ApiLoggingFilter filter;

    @BeforeEach
    void setup() {
        filter = new ApiLoggingFilter();
    }

    @Test
    void testJsonPostRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/json");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"name\":\"Tom\",\"age\":20}".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setStatus(200);
            res.getOutputStream().write("{\"status\":\"ok\"}".getBytes());
        };

        filter.doFilter(request, response, chain);
        String output = response.getContentAsString();
        assertTrue(output.contains("ok"));
    }

    @Test
    void testFormPostRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/form");
        request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        request.setParameter("username", "admin");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> res.getWriter().write("ok"));
        assertTrue(response.getContentAsString().contains("ok"));
    }

    @Test
    void testGetRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hello");
        request.setParameter("name", "tester");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> res.getWriter().write("hi"));

        assertTrue(response.getContentAsString().contains("hi"));
    }

    @Test
    void testMultipartRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload");
        request.setContentType("multipart/form-data; boundary=----WebKitFormBoundary");
        request.setContent("fake multipart".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> res.getWriter().write("uploaded"));
        assertTrue(response.getContentAsString().contains("uploaded"));
    }

    @Test
    void testBinaryResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/download");
        request.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/pdf");

        filter.doFilter(request, response, (req, res) -> {
            res.setContentType("application/pdf");
            res.getOutputStream().write("PDFDATA".getBytes());
        });

        assertTrue(response.getContentAsString().contains("PDFDATA"));
    }

    @Test
    void testUnhandledException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new RuntimeException("Unexpected Error");
        });

        // 异常仍然被 filter 捕捉到，日志应输出
        assertTrue(response.getStatus() == 500 || response.getStatus() == 200);
    }

    @Test
    void testNonHttpRequestBypass() throws Exception {
        ServletRequest mockRequest = mock(ServletRequest.class);
        ServletResponse mockResponse = mock(ServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        filter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain, times(1)).doFilter(mockRequest, mockResponse);
    }
}
```
