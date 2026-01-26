package com.banking.apigateway.filter;

import com.banking.apigateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private RouteValidator validator; // Real object

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        validator = new RouteValidator();
        authenticationFilter = new AuthenticationFilter(validator, jwtUtil, redisTemplate);
    }

    @Test
    void apply_WithSecuredRouteAndValidToken_ShouldForwardRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(jwtUtil.extractUserId(anyString())).thenReturn("123");
        when(chain.filter(any())).thenReturn(Mono.empty());

        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void apply_WithMissingAuthHeader_ShouldThrowException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/accounts")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        GatewayFilter filter = authenticationFilter.apply(new Object());
        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectError(ResponseStatusException.class)
                .verify();
    }
}