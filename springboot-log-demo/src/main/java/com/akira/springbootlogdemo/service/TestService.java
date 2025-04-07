package com.akira.springbootlogdemo.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TestService {

    public Mono<String> processTest() {
        return Mono.just("Test response")
                .map(response -> {
                    // Simulate some processing
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return response;
                });
    }
}
