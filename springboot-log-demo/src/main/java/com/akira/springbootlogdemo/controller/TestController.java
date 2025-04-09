package com.akira.springbootlogdemo.controller;

import com.akira.springbootlogdemo.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/test1")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    /**
     * 更新TestController，确保方法参数包含ServerWebExchange：?
     * 为什么？？？
     * @return
     */
    @GetMapping
    public Mono<String> testEndpoint() {
        return testService.processTest();
    }

    @GetMapping("/web")
    public String testWeb(@RequestParam String userid){
        return "ok";
    }
}