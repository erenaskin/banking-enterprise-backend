package com.banking.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF'i Gateway seviyesinde kapat
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/**").permitAll() // Auth isteklerine izin ver
                        .anyExchange().permitAll() // Veya diğer istekleri de aç (geliştirme ortamı için)
                );
        return http.build();
    }
}
