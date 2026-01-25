package com.banking.apigateway.filter;

import com.banking.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<Object> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ReactiveStringRedisTemplate redisTemplate) {
        super(Object.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                return processSecuredRequest(exchange, chain);
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> processSecuredRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = extractAuthHeader(exchange);
        if (authHeader == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header"));
        }

        return redisTemplate.hasKey("blacklist:" + authHeader)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has been blacklisted"));
                    }
                    return validateAndForward(exchange, chain, authHeader);
                });
    }

    private String extractAuthHeader(ServerWebExchange exchange) {
        if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return null;
        }

        String authHeader = Optional.ofNullable(exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> validateAndForward(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        try {
            jwtUtil.validateToken(token);
            String userId = jwtUtil.extractUserId(token);

            if (userId == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token: userId not found"));
            }

            ServerHttpRequest request = mutateRequest(exchange, userId);
            return chain.filter(exchange.mutate().request(request).build());

        } catch (Exception e) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access to application", e));
        }
    }

    private ServerHttpRequest mutateRequest(ServerWebExchange exchange, String userId) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        return exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .header("X-Correlation-ID", correlationId)
                .build();
    }
}