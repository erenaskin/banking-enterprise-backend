package com.banking.apigateway.filter;

import com.banking.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header"));
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                String finalAuthHeader = authHeader;
                return redisTemplate.hasKey("blacklist:" + finalAuthHeader)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has been blacklisted"));
                            }

                            try {
                                jwtUtil.validateToken(finalAuthHeader);
                                // Token'dan kullanıcı ID'sini al ve header'a ekle
                                String userId = jwtUtil.extractUserId(finalAuthHeader);
                                
                                if (userId == null) {
                                     return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token: userId not found"));
                                }

                                // X-Correlation-ID kontrolü ve eklenmesi
                                String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
                                if (correlationId == null || correlationId.isEmpty()) {
                                    correlationId = UUID.randomUUID().toString();
                                }

                                ServerHttpRequest request = exchange.getRequest()
                                        .mutate()
                                        .header("X-User-Id", userId)
                                        .header("X-Correlation-ID", correlationId)
                                        .build();
                                return chain.filter(exchange.mutate().request(request).build());

                            } catch (Exception e) {
                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access to application", e));
                            }
                        });
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {

    }
}