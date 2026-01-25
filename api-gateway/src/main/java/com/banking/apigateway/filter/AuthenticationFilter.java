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

import java.util.List;
import java.util.Optional;
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

                String authHeader = Optional.ofNullable(exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION))
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.get(0))
                        .orElse(null);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                     // Header var ama formatı yanlış veya boş ise hata dönmeli veya null kalmalı
                     // Mevcut mantıkta null kalırsa aşağıda hata verebilir, o yüzden kontrol ekleyelim
                     if (authHeader == null) {
                         return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header"));
                     }
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