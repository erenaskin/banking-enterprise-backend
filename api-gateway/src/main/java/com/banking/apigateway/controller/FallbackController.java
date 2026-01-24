package com.banking.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/identity-service-fallback")
    public Mono<String> identityServiceFallback() {
        return Mono.just("Identity service is taking too long to respond or is down. Please try again later.");
    }

    @GetMapping("/transaction-service-fallback")
    public Mono<String> transactionServiceFallback() {
        return Mono.just("Transaction service is taking too long to respond or is down. Please try again later.");
    }
}
