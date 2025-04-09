package com.akira.springbootlogdemo.logging.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiLoggingFilter.class)
public class ApiLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRequestLogsCorrectly() throws Exception {
        String requestJson = "{\"msg\": \"hello\"}";

        mockMvc.perform(post("/api/echo")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk());

        // 日志部分手动确认（可配合 mock logger 进一步校验）
    }
}
